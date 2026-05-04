package com.nessaj.ersim.controller;

import com.nessaj.ersim.model.Point;
import com.nessaj.ersim.repository.PointRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/points")
@CrossOrigin(origins = "*")
public class PointController {

    private final PointRepository pointRepository;

    public PointController(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    @GetMapping
    public ResponseEntity<List<Point>> getAllPoints() {
        return ResponseEntity.ok(pointRepository.findAll());
    }

    @GetMapping("/device/{deviceLocalNum}")
    public ResponseEntity<List<Point>> getPointsByDevice(@PathVariable String deviceLocalNum) {
        return ResponseEntity.ok(pointRepository.findByDeviceLocalNum(deviceLocalNum));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Point> getPointById(@PathVariable String id) {
        return pointRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createPoint(@RequestBody Point point) {
        if (point.getPtId() == null || point.getPtId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("点位ID不能为空");
        }
        if (point.getPtName() == null || point.getPtName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("点位名称不能为空");
        }
        if (point.getDeviceLocalNum() == null || point.getDeviceLocalNum().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("关联设备本地编号不能为空");
        }
        if (point.getSignalType() == null || point.getSignalType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("信号类型不能为空");
        }
        if (!isValidSignalType(point.getSignalType())) {
            return ResponseEntity.badRequest().body("信号类型无效，有效值：yx(遥信), yc(遥测), yk(遥控), yt(遥调)");
        }
        if (point.getModelType() == null) {
            return ResponseEntity.badRequest().body("物模型类型不能为空");
        }
        if (!isValidModelType(point.getModelType())) {
            return ResponseEntity.badRequest().body("物模型类型无效，有效值：1(属性), 2(事件), 3(操作)");
        }

        String id = point.getPtId() + "_" + point.getSignalType() + "_" + point.getDeviceLocalNum();
        point.setId(id);

        if (pointRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("点位已存在（ptId+signalType+deviceLocalNum唯一）: " + id);
        }

        point.setModelType(deriveModelType(point.getSignalType()));
        return ResponseEntity.ok(pointRepository.save(point));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePoint(@PathVariable String id, @RequestBody Point point) {
        Optional<Point> existingOpt = pointRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (point.getPtName() == null || point.getPtName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("点位名称不能为空");
        }
        if (point.getDeviceLocalNum() == null || point.getDeviceLocalNum().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("关联设备本地编号不能为空");
        }
        if (point.getSignalType() == null || point.getSignalType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("信号类型不能为空");
        }
        if (!isValidSignalType(point.getSignalType())) {
            return ResponseEntity.badRequest().body("信号类型无效，有效值：yx(遥信), yc(遥测), yk(遥控), yt(遥调)");
        }

        String newId = point.getPtId() + "_" + point.getSignalType() + "_" + point.getDeviceLocalNum();
        if (!id.equals(newId) && pointRepository.existsById(newId)) {
            return ResponseEntity.badRequest().body("点位已存在（ptId+signalType+deviceLocalNum唯一）: " + newId);
        }

        point.setId(newId);
        point.setModelType(deriveModelType(point.getSignalType()));
        return ResponseEntity.ok(pointRepository.save(point));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePoint(@PathVariable String id) {
        if (!pointRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pointRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/devices")
    public ResponseEntity<List<String>> getAvailableDevices() {
        List<String> devices = pointRepository.findAll().stream()
                .map(Point::getDeviceLocalNum)
                .distinct()
                .toList();
        return ResponseEntity.ok(devices);
    }

    private boolean isValidSignalType(String signalType) {
        return "yx".equals(signalType) || "yc".equals(signalType)
            || "yk".equals(signalType) || "yt".equals(signalType);
    }

    private boolean isValidModelType(Integer modelType) {
        return modelType == 1 || modelType == 2 || modelType == 3;
    }

    private Integer deriveModelType(String signalType) {
        return switch (signalType) {
            case "yc" -> 1;
            case "yx" -> 2;
            case "yk", "yt" -> 3;
            default -> 1;
        };
    }
}