// 地址字典业务测试；覆盖层级校验、公开树和删除保护。
package com.campushelp.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.LocationMapper;
import com.campushelp.model.Location;
import com.campushelp.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocationServiceTest {
    @Test
    void buildsEnabledPublicTree() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        when(mapper.findEnabled()).thenReturn(List.of(
            location(1, "station", 0, "北苑菜鸟驿站"),
            location(10, "canteen", 0, "北苑食堂(一饭)"),
            location(20, "floor", 10, "1楼"),
            location(100, "vendor", 20, "沙县小吃"),
            location(200, "building_category", 0, "宿舍楼"),
            location(300, "building", 200, "一号学生宿舍")
        ));

        var tree = service.publicTree();

        assertEquals(1, tree.stations().size());
        assertEquals("北苑菜鸟驿站", tree.stations().get(0).name());
        assertEquals(1, tree.canteens().size());
        assertEquals("1楼", tree.canteens().get(0).floors().get(0).name());
        assertEquals("沙县小吃", tree.canteens().get(0).floors().get(0).vendors().get(0).name());
        assertEquals("一号学生宿舍", tree.buildingCategories().get(0).buildings().get(0).name());
    }

    @Test
    void rejectsMismatchedParentKind() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        when(mapper.findById(10L)).thenReturn(location(10, "canteen", 0, "北苑食堂(一饭)"));

        var error = assertThrows(ApiException.class, () -> service.create(
            new LocationService.SaveRequest("vendor", 10L, "测试档口", 1, true)));

        assertEquals(400, error.status().value());
        verify(mapper, never()).insert(any());
    }

    @Test
    void cannotDeleteNodeWithChildren() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        when(mapper.findById(20L)).thenReturn(location(20, "floor", 10, "1楼"));
        when(mapper.countChildren(20L)).thenReturn(2);

        var error = assertThrows(ApiException.class, () -> service.delete(20L));

        assertEquals(409, error.status().value());
        verify(mapper, never()).delete(20L);
    }

    @Test
    void createsRootStationWithTrimmedName() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        var saved = location(3, "station", 0, "西苑菜鸟驿站");
        when(mapper.countSame("station", 0L, "西苑菜鸟驿站", 0L)).thenReturn(0);
        when(mapper.findById(3L)).thenReturn(saved);
        when(mapper.findAll()).thenReturn(List.of(saved));
        doAnswer(invocation -> {
            Location item = invocation.getArgument(0);
            item.setId(3L);
            return 1;
        }).when(mapper).insert(any());

        var item = service.create(new LocationService.SaveRequest("station", 0L, "  西苑菜鸟驿站  ", 3, true));

        assertEquals(3L, item.id());
        assertEquals("西苑菜鸟驿站", item.name());
        assertEquals("取件驿站", item.kindLabel());
        verify(mapper).insert(argThat(value -> value.getName().equals("西苑菜鸟驿站")));
    }

    @Test
    void updatesTypeWhenNodeHasNoChildren() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        var station = location(3, "station", 0, "西苑菜鸟驿站");
        when(mapper.findById(3L)).thenReturn(station);
        when(mapper.countChildren(3L)).thenReturn(0);
        when(mapper.countSame("canteen", 0L, "西苑食堂", 3L)).thenReturn(0);
        when(mapper.findAll()).thenReturn(List.of(station));

        var item = service.update(3L, new LocationService.UpdateRequest(
            "canteen", 0L, "西苑食堂", 1, true));

        assertEquals("canteen", item.kind());
        assertEquals("食堂", item.kindLabel());
        verify(mapper).update(argThat(value -> "canteen".equals(value.getKind())));
    }

    @Test
    void rejectsTypeChangeWhenChildrenAreIncompatible() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        var canteen = location(10, "canteen", 0, "北苑食堂(一饭)");
        when(mapper.findById(10L)).thenReturn(canteen);
        when(mapper.countChildren(10L)).thenReturn(2);
        when(mapper.findChildren(10L)).thenReturn(List.of(
            location(20, "floor", 10, "1楼"), location(21, "floor", 10, "2楼")));

        var error = assertThrows(ApiException.class, () -> service.update(10L,
            new LocationService.UpdateRequest("station", 0L, "北苑食堂(一饭)", 1, true)));

        assertEquals(409, error.status().value());
        verify(mapper, never()).update(any());
    }

    @Test
    void movesVendorToAnotherFloor() {
        var mapper = mock(LocationMapper.class);
        var service = new LocationService(mapper);
        var canteen = location(10, "canteen", 0, "北苑食堂(一饭)");
        var floor = location(21, "floor", 10, "2楼");
        var vendor = location(100, "vendor", 20, "沙县小吃");
        when(mapper.findById(100L)).thenReturn(vendor);
        when(mapper.findById(21L)).thenReturn(floor);
        when(mapper.countChildren(100L)).thenReturn(0);
        when(mapper.countSame("vendor", 21L, "沙县小吃", 100L)).thenReturn(0);
        when(mapper.findAll()).thenReturn(List.of(canteen, floor, vendor));

        var item = service.update(100L, new LocationService.UpdateRequest(
            "vendor", 21L, "沙县小吃", 1, true));

        assertEquals(21L, item.parentId());
        assertEquals("北苑食堂(一饭) / 2楼", item.parentPath());
        verify(mapper).update(argThat(value -> value.getParentId() == 21L));
    }

    private Location location(long id, String kind, long parentId, String name) {
        var location = new Location();
        location.setId(id);
        location.setKind(kind);
        location.setParentId(parentId);
        location.setName(name);
        location.setSortOrder(1);
        location.setEnabled(true);
        return location;
    }
}
