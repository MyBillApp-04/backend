package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.AppRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppReleaseRepository extends JpaRepository<AppRelease, Long> {
    Optional<AppRelease> findFirstByActiveTrueOrderByVersionCodeDesc();
    List<AppRelease> findAllByOrderByVersionCodeDesc();
    boolean existsByVersionCode(int versionCode);
}
