package org.example.autotrading.order.repository;


import org.example.autotrading.order.entity.MockOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockOrderRepository extends JpaRepository<MockOrderEntity, Long> {
}
