package com.nessaj.ersim.repository;

import com.nessaj.ersim.model.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRepository extends JpaRepository<Point, String> {
    List<Point> findByDeviceLocalNum(String deviceLocalNum);
    List<Point> findBySignalType(String signalType);
    List<Point> findByModelType(Integer modelType);
}