// 轮播业务层；把数据库行转换成前端需要的字段，隐藏启用状态和时间窗等内部字段。
package com.campushelp.content;

import com.campushelp.mapper.BannerMapper;
import com.campushelp.model.Banner;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
// 类/接口职责：轮播业务层；把数据库行转换成前端需要的字段，隐藏启用状态和时间窗等内部字段。
public class BannerService {
    private final BannerMapper banners;

    // BannerService：构造器由 Spring 注入 Mapper；测试时可以传入 mock。
    public BannerService(BannerMapper banners) {
        this.banners = banners;
    }

    // BannerView：前端渲染轮播所需的公开字段；linkValue 由后台配置，不在前端写死。
    public record BannerView(Long id, String imageUrl, String tag, String title, String subtitle,
    String linkType, String linkValue) {}

    // list：只返回当前启用且未过期的轮播，按 sort_order 排序。
    public List<BannerView> list() {
        return banners.listActive().stream().map(this::view).toList();
    }

    // view：过滤掉 enabled、startAt、endAt 等内部字段，避免前端依赖后台实现。
    private BannerView view(Banner banner) {
        return new BannerView(banner.getId(), banner.getImageUrl(), banner.getTag(), banner.getTitle(),
        banner.getSubtitle(), banner.getLinkType(), banner.getLinkValue());
    }
}
