package com.tecdes.smart.app_smart_40.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tecdes.smart.app_smart_40.model.Lamina;

@Repository
public interface LaminaRepository extends JpaRepository<Lamina, Long> {
}
