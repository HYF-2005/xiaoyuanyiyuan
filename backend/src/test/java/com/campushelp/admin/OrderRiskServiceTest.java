// 风险订单规则测试；覆盖自接单、互刷和人工审核状态校验。
package com.campushelp.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.campushelp.mapper.OrderRiskMapper;
import com.campushelp.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderRiskServiceTest {
    @Test
    void flagsSelfOrderAsHighRisk() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        var order = signal(1L, 10L, 10L, LocalDateTime.now().minusMinutes(10));
        order.setAcceptedAt(LocalDateTime.now().minusMinutes(5));
        order.setCompletedAt(LocalDateTime.now().minusMinutes(2));
        when(mapper.signals()).thenReturn(List.of(order));
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        var result = service.scan();

        assertEquals(1, result.scanned());
        assertEquals(1, result.flagged());
        assertEquals(1, result.high());
        verify(mapper).upsert(eq(1L), eq(100), eq("high"),
            argThat(flags -> flags.contains("发布者与接单者相同")));
    }

    @Test
    void flagsReciprocalBurstWithinTenMinutes() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        var orders = reciprocalBurst(6, 5);
        when(mapper.signals()).thenReturn(orders);
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        service.scan();

        verify(mapper, times(6)).upsert(anyLong(), eq(75), eq("high"),
            argThat(flags -> flags.contains("1 天内互相下单超过 5 笔且接单间隔≤10 分钟")));
    }

    @Test
    void doesNotFlagFiveMutualOrders() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        when(mapper.signals()).thenReturn(reciprocalBurst(5, 5));
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        service.scan();

        verify(mapper, times(5)).upsert(anyLong(), eq(0), eq("none"), anyString());
    }

    @Test
    void doesNotFlagMutualOrdersWithLongGap() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        when(mapper.signals()).thenReturn(reciprocalBurst(6, 11));
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        service.scan();

        verify(mapper, times(6)).upsert(anyLong(), eq(0), eq("none"), anyString());
    }

    @Test
    void flagsFastProofSubmission() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        var order = signal(1L, 10L, 20L, LocalDateTime.now().minusMinutes(5));
        order.setAcceptedAt(LocalDateTime.now().minusMinutes(4));
        order.setSubmittedAt(LocalDateTime.now().minusMinutes(4).plusSeconds(30));
        order.setCompletedAt(LocalDateTime.now().minusMinutes(3));
        when(mapper.signals()).thenReturn(List.of(order));
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        service.scan();

        verify(mapper).upsert(eq(1L), eq(25), eq("low"),
            argThat(flags -> flags.contains("接单后 30 秒内完成/提交")));
    }

    @Test
    void flagsBurstPublisherAcrossFullDayWindow() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);
        var now = LocalDateTime.now();
        var orders = new java.util.ArrayList<OrderRiskRow.Signal>();
        for (int index = 0; index < 6; index++) {
            orders.add(signal(index + 1L, 10L, null, now.minusHours(6 - index)));
        }
        when(mapper.signals()).thenReturn(orders);
        when(mapper.upsert(anyLong(), anyInt(), anyString(), anyString())).thenReturn(1);

        service.scan();

        verify(mapper, times(6)).upsert(anyLong(), eq(30), eq("medium"),
            argThat(flags -> flags.contains("24 小时内发布或接单超过 5 笔")));
    }

    @Test
    void rejectsUnknownReviewStatus() {
        var mapper = mock(OrderRiskMapper.class);
        var service = new OrderRiskService(mapper);

        var error = assertThrows(ApiException.class, () -> service.review(1L,
            new OrderRiskService.ReviewRequest("随便处理", null), 7L));

        assertEquals(400, error.status().value());
        verify(mapper, never()).review(anyLong(), anyString(), anyString(), anyLong());
    }

    private List<OrderRiskRow.Signal> reciprocalBurst(int count, int gapMinutes) {
        var start = LocalDateTime.now().minusHours(2);
        var orders = new java.util.ArrayList<OrderRiskRow.Signal>();
        for (int index = 0; index < count; index++) {
            boolean forward = index % 2 == 0;
            var order = signal(index + 1L, forward ? 10L : 20L, forward ? 20L : 10L,
                start.plusMinutes((long) index * gapMinutes).minusMinutes(1));
            var accepted = start.plusMinutes((long) index * gapMinutes);
            order.setAcceptedAt(accepted);
            order.setSubmittedAt(accepted.plusMinutes(5));
            order.setCompletedAt(accepted.plusMinutes(8));
            orders.add(order);
        }
        return orders;
    }

    private OrderRiskRow.Signal signal(long id, long publisherId, Long runnerId, LocalDateTime createdAt) {
        var order = new OrderRiskRow.Signal();
        order.setId(id);
        order.setType("parcel");
        order.setTitle("测试订单");
        order.setStatus("已完成");
        order.setReward(new BigDecimal("8.00"));
        order.setPublisherId(publisherId);
        order.setRunnerId(runnerId);
        order.setProofFile("proof.jpg");
        order.setCreatedAt(createdAt);
        order.setPublisherCreatedAt(createdAt.minusDays(30));
        order.setRunnerCreatedAt(createdAt.minusDays(30));
        return order;
    }
}
