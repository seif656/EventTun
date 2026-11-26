package tn.eventtun.eventservice.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "seat_reservations", uniqueConstraints = @UniqueConstraint(name = "uk_reservation_order_category", columnNames = {"order_id", "category_id"}))
public class SeatReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "category_id", nullable = false) private Long categoryId;
    @Column(nullable = false) private Integer quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.HELD;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public SeatReservation() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
