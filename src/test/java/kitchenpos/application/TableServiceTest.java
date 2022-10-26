package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import javax.transaction.Transactional;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import kitchenpos.fixtures.OrderFixtures;
import kitchenpos.fixtures.OrderTableFixtures;
import kitchenpos.fixtures.TableGroupFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class TableServiceTest {

    @Autowired
    private TableService tableService;

    @Autowired
    private OrderTableDao orderTableDao;

    @Autowired
    private TableGroupDao tableGroupDao;

    @Autowired
    private OrderDao orderDao;

    @Test
    @DisplayName("주문 테이블을 생성한다")
    void create() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createEmptyTable(null);

        // when
        final OrderTable saved = tableService.create(orderTable);

        // then
        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.isEmpty()).isTrue(),
                () -> assertThat(saved.getTableGroupId()).isNull()
        );
    }

    @Test
    @DisplayName("모든 주문 테이블을 조회한다")
    void list() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createWithGuests(null, 2);
        final OrderTable saved = tableService.create(orderTable);

        // when
        final List<OrderTable> orderTables = tableService.list();
        // then
        assertAll(
                () -> assertThat(orderTables).hasSizeGreaterThanOrEqualTo(1),
                () -> assertThat(orderTables).extracting("id")
                        .contains(saved.getId())
        );
    }

    @Test
    @DisplayName("주문 테이블의 주문 가능 여부를 변경한다")
    void changeEmpty() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createEmptyTable(null);
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable notEmptyOrderTable = OrderTableFixtures.createWithGuests(null, 2);

        final Order order = OrderFixtures.COMPLETION_ORDER.createWithOrderTableId(savedOrderTable.getId());
        orderDao.save(order);

        // when
        final OrderTable changedOrderTable = tableService.changeEmpty(savedOrderTable.getId(), notEmptyOrderTable);

        // then
        assertAll(
                () -> assertThat(changedOrderTable.getId()).isEqualTo(savedOrderTable.getId()),
                () -> assertThat(changedOrderTable.getTableGroupId()).isEqualTo(savedOrderTable.getTableGroupId()),
                () -> assertThat(changedOrderTable.getNumberOfGuests()).isEqualTo(savedOrderTable.getNumberOfGuests()),
                () -> assertThat(changedOrderTable.isEmpty()).isFalse()
        );
    }

    @Test
    @DisplayName("주문 테이블이 존재하지 않으면 주문 테이블의 주문 가능 여부를 변경할 때 예외가 발생한다")
    void changeEmptyExceptionNotExistOrderTable() {
        // given
        final OrderTable notEmptyOrderTable = OrderTableFixtures.createWithGuests(null, 2);

        // when, then
        assertThatThrownBy(() -> tableService.changeEmpty(-1L, notEmptyOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 테이블이 단체로 지정되어 있디면 주문 테이블의 주문 가능 여부를 변경할 때 예외가 발생한다")
    void changeEmptyExceptionGroupedOrderTable() {
        // given
        final TableGroup tableGroup = TableGroupFixtures.create();
        final TableGroup savedTableGroup = tableGroupDao.save(tableGroup);

        final OrderTable orderTable = OrderTableFixtures.createEmptyTable(savedTableGroup.getId());
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable notEmptyOrderTable = OrderTableFixtures.createWithGuests(null, 2);

        // when, then
        assertThatThrownBy(() -> tableService.changeEmpty(savedOrderTable.getId(), notEmptyOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COOKING_ORDER", "MEAL_ORDER"})
    @DisplayName("주문이 상태가 COOKING, MEAL인 경우 주문 테이블의 주문 가능 여부를 변경할 때 예외가 발생한다")
    void changeEmptyExceptionNotCompletionOrder(final OrderFixtures orderFixtures) {
        // given
        final OrderTable orderTable = OrderTableFixtures.createEmptyTable(null);
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable notEmptyOrderTable = OrderTableFixtures.createWithGuests(null, 2);

        final Order order = orderFixtures.createWithOrderTableId(savedOrderTable.getId());
        orderDao.save(order);

        // when, then
        assertThatThrownBy(() -> tableService.changeEmpty(savedOrderTable.getId(), notEmptyOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 테이블의 손님 숫자를 변경한다")
    void changeNumberOfGuests() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createWithGuests(null, 2);
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable threeGuestsOrderTable = OrderTableFixtures.createWithGuests(null, 3);

        // when
        final OrderTable changedOrderTable = tableService.changeNumberOfGuests(savedOrderTable.getId(),
                threeGuestsOrderTable);

        // then
        assertAll(
                () -> assertThat(changedOrderTable.getId()).isEqualTo(savedOrderTable.getId()),
                () -> assertThat(changedOrderTable.getTableGroupId()).isEqualTo(savedOrderTable.getTableGroupId()),
                () -> assertThat(changedOrderTable.isEmpty()).isFalse(),
                () -> assertThat(changedOrderTable.getNumberOfGuests()).isEqualTo(3)
        );
    }

    @Test
    @DisplayName("변경하려는 손님 숫자가 음수이면 주문 테이블의 손님 숫자를 변경할 때 예외가 발생한다")
    void changeNumberOfGuestsExceptionNegativeNumberOfGuests() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createWithGuests(null, 2);
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable threeGuestsOrderTable = OrderTableFixtures.createWithGuests(null, -1);

        // when, then
        assertThatThrownBy(() -> tableService.changeNumberOfGuests(savedOrderTable.getId(), threeGuestsOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("변경하려는 주문 테이블이 존재하지 않으면 주문 테이블의 손님 숫자를 변경할 때 예외가 발생한다")
    void changeNumberOfGuestsExceptionNotExistOrderTable() {
        // given
        final OrderTable threeGuestsOrderTable = OrderTableFixtures.createEmptyTable(null);

        // when, then
        assertThatThrownBy(() -> tableService.changeNumberOfGuests(-1L, threeGuestsOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("변경하려는 주문 테이블이 주문을 동록할 수 없다면 주문 테이블의 손님 숫자를 변경할 때 예외가 발생한다")
    void changeNumberOfGuestsExceptionNotEmptyOrderTable() {
        // given
        final OrderTable orderTable = OrderTableFixtures.createEmptyTable(null);
        final OrderTable savedOrderTable = orderTableDao.save(orderTable);

        final OrderTable threeGuestsOrderTable = OrderTableFixtures.createWithGuests(null, 1);

        // when, then
        assertThatThrownBy(() -> tableService.changeNumberOfGuests(savedOrderTable.getId(), threeGuestsOrderTable))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
