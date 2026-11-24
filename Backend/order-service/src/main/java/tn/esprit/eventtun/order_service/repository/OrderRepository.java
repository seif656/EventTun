package tn.esprit.eventtun.order_service.repository;

import tn.esprit.eventtun.order_service.entity.Order;
import tn.esprit.eventtun.order_service.enums.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    @Override
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = "items")
    @Query("""
        SELECT DISTINCT o FROM Order o
        WHERE (:userId IS NULL OR o.userId = :userId)
        AND (:status IS NULL OR o.status = :status)
        ORDER BY o.createdAt DESC
    """)
    List<Order> searchOrders(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );
}