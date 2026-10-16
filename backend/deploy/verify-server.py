"""Verify the deployed API locally on the server with isolated test accounts."""
import datetime
import io
import json
import secrets
import urllib.error
import urllib.request

# 服务端本机校验地址；本脚本运行会新建测试账号/订单并最后停用账号，不是只读检查。
BASE = "http://127.0.0.1:8080/api"


# HTTP 请求助手：有 data 时提交 POST；支持 JSON 和照片字节，断言响应状态。
def call(path, token=None, data=None, expected=200, raw=False, content_type=None):
    body = None if data is None else data if isinstance(data, bytes) else json.dumps(data, ensure_ascii=False).encode()
    headers = {"Content-Type": content_type or "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(BASE + path, data=body, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status, result = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, result = error.code, error.read()
    assert status == expected, f"{path}: expected {expected}, got {status}"
    return result if raw else json.loads(result)


# 完整部署验收：注册登录 → 发布/接单 → 上传/下载凭证 → 确认/取消 → 信用/收入 → 反馈/注销。
def main():
    # 生成测试 JPEG 需要 Pillow；不在小程序前端运行。
    from PIL import Image
    assert call("/health")["status"] == "ok"
    call("/me", expected=401)
    # 部署预期已关闭演示码，所以应返回 501；本机演示开启时不能照搬这一断言。
    call("/auth/code", data={"phone": "13800138000", "purpose": "bind"}, expected=501)
    # 使用随机后缀隔离测试账号，避免影响真实账号。
    suffix = secrets.token_hex(4)
    password = "DeployA" + secrets.token_hex(8)
    publisher_name, runner_name = "verify_pub_" + suffix, "verify_run_" + suffix
    publisher = call("/auth/register", data={"username": publisher_name, "password": password, "displayName": "部署验证发布者"})
    runner = call("/auth/register", data={"username": runner_name, "password": password, "displayName": "部署验证接单者"})
    pt, rt = publisher["token"], runner["token"]
    assert call("/auth/login", data={"username": publisher_name, "password": password})["userId"] == publisher["userId"]
    tomorrow = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()
    deadline = {"deadlineDate": tomorrow, "deadlineTime": "23:59", "deliveryBuilding": "部署验证楼", "deliveryAddress": "测试地址"}
    parcel = call("/orders", pt, {**deadline, "type": "parcel", "station": "部署验证驿站", "packages": [{"code": "TEST-001", "size": "小件"}], "reward": 3.1})
    assert any(o["id"] == parcel["id"] for o in call("/orders/market?type=parcel", rt))
    assert call(f'/orders/{parcel["id"]}', rt)["packages"] == []
    assert call(f'/orders/{parcel["id"]}/accept', rt, {})["status"] == "已接单"
    # 在内存生成小 JPEG，随后构造 multipart/form-data 检查照片上传。
    buffer = io.BytesIO()
    Image.new("RGB", (16, 16), (0, 199, 104)).save(buffer, format="JPEG")
    boundary = "CampusHelp" + secrets.token_hex(8)
    upload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="verify.jpg"\r\nContent-Type: image/jpeg\r\n\r\n'.encode()
              + buffer.getvalue() + f'\r\n--{boundary}--\r\n'.encode())
    assert call(f'/orders/{parcel["id"]}/proof', rt, upload, content_type="multipart/form-data; boundary=" + boundary)["status"] == "待确认"
    call(f'/orders/{parcel["id"]}/proof', pt, raw=True)
    assert call(f'/orders/{parcel["id"]}/confirm', pt, {})["status"] == "已完成"
    # 检查完成收入统计，不代表发生了真实支付。
    assert call("/me", rt)["incomeTotal"] == 3.1
    assert call("/me/credit", rt)["score"] == 62
    assert len(call("/me/transactions", rt)) == 1
    meal = call("/orders", pt, {**deadline, "type": "meal", "restaurant": "部署验证餐厅", "codes": ["TEST-002"], "reward": 1})
    call(f'/orders/{meal["id"]}/accept', rt, {})
    assert call(f'/orders/{meal["id"]}/cancel', rt, {})["status"] == "已取消"
    assert call("/me/credit", rt)["score"] == 58
    assert call("/me", rt)["incomeTotal"] == 3.1
    assert any(r["kind"] == "refund" for r in call("/me/transactions", pt))
    call("/feedback", pt, {"content": "部署验证：订单、拍照、信用分与收入接口检查。"})
    # 验证完成后停用专用测试账号；历史验证记录仍留数据库。
    call("/me/deactivate", pt, {"password": password})
    call("/me/deactivate", rt, {"password": password})
    call("/me", pt, expected=401)
    print("PASS: registration/login, authorization, parcel/meal, photo upload/download, confirmation/cancellation, credit, income, transactions, feedback, account closure")
    print("Verification accounts are deactivated; verification records are retained and labelled.")


# 仅直接运行本脚本时执行验收，不会在被导入时自动操作数据库。
if __name__ == "__main__":
    main()
