package com.r2s.uam.auth.repository;

import com.r2s.uam.auth.entity.Device;
import com.r2s.uam.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByUser(User user);

    List<Device> findByUserAndIsActiveTrue(User user);

    Optional<Device> findByUserAndIdAndIsActiveTrue(User user, UUID deviceId);

    @Modifying
    @Query("UPDATE Device d SET d.isActive = false WHERE d.user = :user")
    void deactivateAllUserDevices(@Param("user") User user);

    @Modifying
    @Query("UPDATE Device d SET d.isActive = false WHERE d.id = :deviceId")
    void deactivateDevice(@Param("deviceId") UUID deviceId);
}
