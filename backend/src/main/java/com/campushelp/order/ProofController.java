// 订单照片上传/下载入口；校验图片与大小，转成 JPEG 保存，访问必须经过身份与订单权限检查。
package com.campushelp.order;

import com.campushelp.auth.AuthInterceptor;
import com.campushelp.model.HelpOrder;
import com.campushelp.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.ImageReader;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/orders/{id}/proof")
// 类/接口职责：订单照片上传/下载入口；校验图片与大小，转成 JPEG 保存，访问必须经过身份与订单权限检查。
public class ProofController {
    private final OrderService orders;
    // uploads：服务器上传目录的绝对规范化路径；final 引用在构造后不再换成别的对象。
    private final Path uploads;

    // ProofController：构造器由 Spring 注入所需依赖（测试中可手动创建）；不在每次请求中重复 new 数据库工具。
    public ProofController(OrderService orders, @Value("${app.uploads-dir}") String uploadsDir) {
        this.orders = orders;
        this.uploads = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // upload：接收 multipart 的 file；限制 5MB 与像素数，检查真实图像，透明图铺白底，随机文件名转 JPEG；业务更新失败清理本次文件。
    public OrderService.OrderView upload(HttpServletRequest request, @PathVariable Long id,
    @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = AuthInterceptor.current(request).getId();
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw bad("照片不能超过 5MB 且不能为空");
        BufferedImage image;
        try (var input = file.getInputStream(); var imageInput = ImageIO.createImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw bad("请上传有效的图片文件");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000) throw bad("照片分辨率过大，请压缩后上传");
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        // 透明 PNG 转 JPEG 前铺白底，避免透明区域变黑。
        if (image.getColorModel().hasAlpha()) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = rgb.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = rgb;
        }
        // 随机文件名由服务器生成，原始文件名不参与路径拼接。
        String name = UUID.randomUUID() + ".jpg";
        Files.createDirectories(uploads);
        Path target = uploads.resolve(name);
        // 统一转成 JPEG，文件名由服务端生成；原始文件名不参与路径拼接。
        if (!ImageIO.write(image, "jpg", target.toFile())) throw bad("照片格式不支持");
        try {
            return orders.submitProof(id, userId, name);
        } catch (RuntimeException error) {
            // 这只清理本次业务失败后新生成的照片，不删除用户其他照片。
            Files.deleteIfExists(target);
            throw error;
        }
    }

    @GetMapping
    // image：先验证照片权限；规范化路径并确认在上传目录内，返回 image/jpeg 文件资源。
    public ResponseEntity<Resource> image(HttpServletRequest request, @PathVariable Long id) throws IOException {
        HelpOrder order = orders.proofOrder(id, AuthInterceptor.current(request).getId());
        Path path = uploads.resolve(order.getProofFile()).normalize();
        if (!path.startsWith(uploads) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "照片不存在");
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new UrlResource(path.toUri()));
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
