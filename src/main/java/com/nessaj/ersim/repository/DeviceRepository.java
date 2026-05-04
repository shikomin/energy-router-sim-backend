package com.nessaj.ersim.repository;

import com.nessaj.ersim.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByType(String type);
    List<Device> findByDeviceLocalNum(String deviceLocalNum);
}