package kitchenpos.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kitchenpos.dao.MenuGroupRepository;
import kitchenpos.dao.MenuProductRepository;
import kitchenpos.dao.MenuRepository;
import kitchenpos.dao.ProductRepository;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Product;
import kitchenpos.exception.NotFoundMenuGroupException;
import kitchenpos.exception.NotFoundProductException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuProductRepository menuProductRepository;
    private final ProductRepository productRepository;

    public MenuService(
            final MenuRepository menuRepository,
            final MenuGroupRepository menuGroupRepository,
            final MenuProductRepository menuProductRepository,
            final ProductRepository productRepository
    ) {
        this.menuRepository = menuRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.menuProductRepository = menuProductRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Menu create(final Menu menu) {
        if (!menuGroupRepository.existsById(menu.getMenuGroupId())) {
            throw new NotFoundMenuGroupException();
        }

        validateMenuPrice(menu);
        final Menu savedMenu = menuRepository.save(menu);

        final List<MenuProduct> savedMenuProducts = saveMenuProducts(menu.getMenuProducts(), savedMenu);

        savedMenu.updateMenuProducts(savedMenuProducts);
        return savedMenu;
    }

    private void validateMenuPrice(final Menu menu) {
        BigDecimal sum = BigDecimal.ZERO;
        for (final MenuProduct menuProduct : menu.getMenuProducts()) {
            final Product product = productRepository.findById(menuProduct.getProductId())
                    .orElseThrow(NotFoundProductException::new);
            sum = sum.add(product.calculateTotalPrice(menuProduct.getQuantity()));
        }
        menu.validatePrice(sum);
    }

    private List<MenuProduct> saveMenuProducts(final List<MenuProduct> menuProducts, final Menu menu) {
        final List<MenuProduct> savedMenuProducts = new ArrayList<>();
        for (final MenuProduct menuProduct : menuProducts) {
            menuProduct.updateMenu(menu);
            savedMenuProducts.add(menuProductRepository.save(menuProduct));
        }
        return savedMenuProducts;
    }

    public List<Menu> list() {
        return menuRepository.findAll();
    }
}
