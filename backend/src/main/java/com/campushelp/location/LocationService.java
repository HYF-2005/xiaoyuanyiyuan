// 地址字典业务；后台维护层级，发布页按启用状态读取对应树。
package com.campushelp.location;

import com.campushelp.mapper.LocationMapper;
import com.campushelp.model.Location;
import com.campushelp.web.ApiException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    public static final String STATION = "station";
    public static final String CANTEEN = "canteen";
    public static final String FLOOR = "floor";
    public static final String VENDOR = "vendor";
    public static final String BUILDING_CATEGORY = "building_category";
    public static final String BUILDING = "building";

    private static final Set<String> KINDS = Set.of(STATION, CANTEEN, FLOOR, VENDOR, BUILDING_CATEGORY, BUILDING);
    private static final Map<String, String> LABELS = Map.of(
        STATION, "取件驿站",
        CANTEEN, "食堂",
        FLOOR, "食堂楼层",
        VENDOR, "取餐档口",
        BUILDING_CATEGORY, "建筑分类",
        BUILDING, "送达建筑"
    );
    private static final List<String> COUNT_ORDER = List.of(
        STATION, CANTEEN, FLOOR, VENDOR, BUILDING_CATEGORY, BUILDING
    );

    private final LocationMapper locations;

    public LocationService(LocationMapper locations) {
        this.locations = locations;
    }

    public record SaveRequest(String kind, Long parentId, String name, Integer sortOrder, Boolean enabled) {}
    public record UpdateRequest(String kind, Long parentId, String name, Integer sortOrder, Boolean enabled) {}

    public record Item(Long id, String kind, String kindLabel, Long parentId, String parentName,
        String parentPath, String name, int sortOrder, boolean enabled, int childCount,
        LocalDateTime updatedAt) {}

    public record Overview(List<Item> items, long total, Map<String, Long> counts) {}

    public record Node(Long id, String name) {}
    public record Floor(Long id, String name, List<Node> vendors) {}
    public record Canteen(Long id, String name, List<Floor> floors) {}
    public record BuildingCategory(Long id, String name, List<Node> buildings) {}
    public record PublicLocations(List<Node> stations, List<Canteen> canteens,
        List<BuildingCategory> buildingCategories) {}

    // list：一次返回全部地址；当前校园地址规模很小，后台筛选在浏览器完成。
    public Overview list() {
        List<Location> all = locations.findAll();
        Map<Long, Location> byId = all.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Location::getId, Function.identity()));
        Map<Long, Long> childCounts = all.stream()
            .filter(item -> item.getParentId() != null && item.getParentId() > 0)
            .collect(Collectors.groupingBy(Location::getParentId, Collectors.counting()));
        List<Item> items = all.stream().map(item -> item(item, byId, childCounts)).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String kind : COUNT_ORDER) {
            counts.put(kind, all.stream().filter(item -> kind.equals(item.getKind())).count());
        }
        return new Overview(items, all.size(), counts);
    }

    // publicTree：只输出启用节点；父节点停用时子节点不会被拼进树。
    public PublicLocations publicTree() {
        List<Location> enabled = locations.findEnabled();
        Map<Long, List<Location>> byParent = enabled.stream()
            .filter(item -> item.getParentId() != null)
            .collect(Collectors.groupingBy(Location::getParentId));

        List<Node> stations = children(enabled, 0L, STATION).stream()
            .map(item -> new Node(item.getId(), item.getName())).toList();
        List<Canteen> canteens = children(enabled, 0L, CANTEEN).stream().map(canteen -> {
            List<Floor> floors = byParent.getOrDefault(canteen.getId(), List.of()).stream()
                .filter(item -> FLOOR.equals(item.getKind()))
                .map(floor -> new Floor(floor.getId(), floor.getName(),
                    byParent.getOrDefault(floor.getId(), List.of()).stream()
                        .filter(item -> VENDOR.equals(item.getKind()))
                        .map(item -> new Node(item.getId(), item.getName())).toList()))
                .toList();
            return new Canteen(canteen.getId(), canteen.getName(), floors);
        }).toList();
        List<BuildingCategory> categories = children(enabled, 0L, BUILDING_CATEGORY).stream()
            .map(category -> new BuildingCategory(category.getId(), category.getName(),
                byParent.getOrDefault(category.getId(), List.of()).stream()
                    .filter(item -> BUILDING.equals(item.getKind()))
                    .map(item -> new Node(item.getId(), item.getName())).toList()))
            .toList();
        return new PublicLocations(stations, canteens, categories);
    }

    public Item create(SaveRequest request) {
        if (request == null) throw bad("请填写地址信息");
        String kind = normalizedKind(request.kind());
        String name = cleanName(request.name());
        long parentId = request.parentId() == null ? 0 : request.parentId();
        validateParent(kind, parentId);
        if (locations.countSame(kind, parentId, name, 0L) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "同一上级下已存在同名地址");
        }
        Location location = new Location();
        location.setKind(kind);
        location.setParentId(parentId);
        location.setName(name);
        location.setSortOrder(sortOrder(request.sortOrder()));
        location.setEnabled(request.enabled() == null || request.enabled());
        locations.insert(location);
        return findItem(location.getId());
    }

    // update：允许改名、排序、启停，也允许调整类型/上级；有子项时必须保持层级兼容。
    public Item update(Long id, UpdateRequest request) {
        Location location = require(id);
        if (request == null) throw bad("请填写地址信息");
        String kind = request.kind() == null || request.kind().isBlank()
            ? location.getKind() : normalizedKind(request.kind());
        long parentId = request.parentId() == null ? location.getParentId() : request.parentId();
        validateParent(kind, parentId);
        validateChildren(id, kind);
        String name = cleanName(request.name());
        if (locations.countSame(kind, parentId, name, id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "同一上级下已存在同名地址");
        }
        location.setKind(kind);
        location.setParentId(parentId);
        location.setName(name);
        location.setSortOrder(sortOrder(request.sortOrder()));
        location.setEnabled(request.enabled() == null || request.enabled());
        locations.update(location);
        return findItem(id);
    }

    // 修改类型时，已有子项必须仍然是新类型的合法下级；移动上级时子项自身不受影响。
    private void validateChildren(Long id, String kind) {
        int count = locations.countChildren(id);
        if (count == 0) return;
        String allowed = switch (kind) {
            case CANTEEN -> FLOOR;
            case FLOOR -> VENDOR;
            case BUILDING_CATEGORY -> BUILDING;
            default -> null;
        };
        if (allowed == null || locations.findChildren(id).stream()
            .anyMatch(child -> !allowed.equals(child.getKind()))) {
            throw new ApiException(HttpStatus.CONFLICT, "该地址下存在不兼容的子项，无法修改类型");
        }
    }

    public Item detail(Long id) {
        return findItem(id);
    }

    public void delete(Long id) {
        require(id);
        if (locations.countChildren(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "请先删除或停用下级地址");
        }
        locations.delete(id);
    }

    private void validateParent(String kind, long parentId) {
        if (parentId < 0) throw bad("上级地址无效");
        if (Set.of(STATION, CANTEEN, BUILDING_CATEGORY).contains(kind)) {
            if (parentId != 0) throw bad("该类型地址不能有上级");
            return;
        }
        if (parentId == 0) throw bad("请选择上级地址");
        Location parent = locations.findById(parentId);
        if (parent == null) throw bad("上级地址不存在");
        String expected = switch (kind) {
            case FLOOR -> CANTEEN;
            case VENDOR -> FLOOR;
            case BUILDING -> BUILDING_CATEGORY;
            default -> throw bad("地址类型无效");
        };
        if (!expected.equals(parent.getKind())) throw bad("上级地址类型不匹配");
    }

    private List<Location> children(List<Location> all, Long parentId, String kind) {
        return all.stream()
            .filter(item -> Objects.equals(parentId, item.getParentId()) && kind.equals(item.getKind()))
            .toList();
    }

    private Item findItem(Long id) {
        Location location = require(id);
        List<Location> all = locations.findAll();
        Map<Long, Location> byId = all.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(Location::getId, Function.identity()));
        Map<Long, Long> childCounts = all.stream()
            .filter(item -> item.getParentId() != null && item.getParentId() > 0)
            .collect(Collectors.groupingBy(Location::getParentId, Collectors.counting()));
        return item(location, byId, childCounts);
    }

    private Item item(Location location, Map<Long, Location> byId, Map<Long, Long> childCounts) {
        Location parent = byId.get(location.getParentId());
        String parentName = parent == null ? "—" : parent.getName();
        String parentPath = parentName;
        if (parent != null && VENDOR.equals(location.getKind())) {
            Location grandparent = byId.get(parent.getParentId());
            if (grandparent != null) parentPath = grandparent.getName() + " / " + parent.getName();
        }
        return new Item(location.getId(), location.getKind(),
            LABELS.getOrDefault(location.getKind(), location.getKind()),
            location.getParentId(), parentName, parentPath, location.getName(),
            location.getSortOrder() == null ? 0 : location.getSortOrder(),
            Boolean.TRUE.equals(location.getEnabled()),
            childCounts.getOrDefault(location.getId(), 0L).intValue(),
            location.getUpdatedAt());
    }

    private Location require(Long id) {
        Location location = id == null ? null : locations.findById(id);
        if (location == null) throw new ApiException(HttpStatus.NOT_FOUND, "地址不存在");
        return location;
    }

    private static String normalizedKind(String kind) {
        if (kind == null || !KINDS.contains(kind)) throw bad("地址类型无效");
        return kind;
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) throw bad("请填写地址名称");
        String cleaned = name.trim();
        if (cleaned.length() > 80) throw bad("地址名称不能超过 80 个字符");
        return cleaned;
    }

    private static int sortOrder(Integer value) {
        int order = value == null ? 0 : value;
        if (order < 0 || order > 9999) throw bad("排序需在 0–9999 之间");
        return order;
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
