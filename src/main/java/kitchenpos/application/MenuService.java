package kitchenpos.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kitchenpos.dao.MenuDao;
import kitchenpos.dao.MenuGroupDao;
import kitchenpos.dao.MenuProductDao;
import kitchenpos.dao.ProductDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    private final MenuDao menuDao;
    private final MenuGroupDao menuGroupDao;
    private final MenuProductDao menuProductDao;
    private final ProductDao productDao;

    public MenuService(
            final MenuDao menuDao,
            final MenuGroupDao menuGroupDao,
            final MenuProductDao menuProductDao,
            final ProductDao productDao
    ) {
        this.menuDao = menuDao;
        this.menuGroupDao = menuGroupDao;
        this.menuProductDao = menuProductDao;
        this.productDao = productDao;
    }

    @Transactional
    public Menu create(final Menu menu) {
        if (!menuGroupDao.existsById(menu.getMenuGroupId())) {
            throw new IllegalArgumentException();
        }
        final List<MenuProduct> menuProducts = menu.getMenuProducts();
        validateMenuPrice(menu, menuProducts);
        final Menu savedMenu = menuDao.save(menu);

        final Long menuId = savedMenu.getId();
        final List<MenuProduct> savedMenuProducts = new ArrayList<>();
        for (final MenuProduct menuProduct : menuProducts) {
            menuProduct.setMenuId(menuId);
            savedMenuProducts.add(menuProductDao.save(menuProduct));
        }
        return new Menu(savedMenu.getId(), savedMenu.getName(), savedMenu.getPrice(), savedMenu.getMenuGroupId(),
                savedMenuProducts);
    }

    private void validateMenuPrice(final Menu menu, final List<MenuProduct> menuProducts) {
        BigDecimal sum = BigDecimal.ZERO;
        for (final MenuProduct menuProduct : menuProducts) {
            final Product product = productDao.findById(menuProduct.getProductId())
                    .orElseThrow(IllegalArgumentException::new);
            sum = sum.add(product.calculateTotalPrice(menuProduct.getQuantity()));
        }
        menu.validatePrice(sum);
    }

    public List<Menu> list() {
        final List<Menu> menus = new ArrayList<>();
        for (final Menu menu : menuDao.findAll()) {
            menus.add(new Menu(menu.getId(), menu.getName(), menu.getPrice(), menu.getMenuGroupId(),
                    menuProductDao.findAllByMenuId(menu.getId())));
        }
        return menus;
    }
}
