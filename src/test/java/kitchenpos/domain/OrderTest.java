package kitchenpos.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kitchenpos.exception.OrderCompletionException;
import kitchenpos.exception.OrderLineItemEmptyException;
import kitchenpos.exception.OrderLineItemSizeException;
import kitchenpos.fixtures.OrderFixtures;
import kitchenpos.fixtures.OrderLineItemFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    @DisplayName("주문 항목은 비어있을 수 없다")
    void validateNotEmptyOrderLineItems() {
        final Order order = OrderFixtures.COOKING_ORDER.create();

        assertThatThrownBy(order::validateNotEmptyOrderLineItems)
                .isExactlyInstanceOf(OrderLineItemEmptyException.class);
    }

    @Test
    @DisplayName("주문 항목의 메뉴 개수와 실제 존재하는 메뉴 개수는 일치해야 한다")
    void validateExistMenu() {
        final OrderLineItem orderLineItem = OrderLineItemFixtures.create(null, 1L, 2);
        final Order order = OrderFixtures.COOKING_ORDER.createWithOrderTableIdAndOrderLineItems(1L, orderLineItem);

        assertThatThrownBy(() -> order.validateOrderLineItemSize(2))
                .isExactlyInstanceOf(OrderLineItemSizeException.class);
    }

    @Test
    @DisplayName("주문이 이미 완료된 상태라면 상태를 변경할 수 없다")
    void validateOrderNotCompletion() {
        final Order order = OrderFixtures.COMPLETION_ORDER.create();

        assertThatThrownBy(() -> order.updateOrderStatus(OrderFixtures.COOKING_ORDER.name()))
                .isExactlyInstanceOf(OrderCompletionException.class);
    }
}
