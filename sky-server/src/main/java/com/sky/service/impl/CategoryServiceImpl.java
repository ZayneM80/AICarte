package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.vo.CategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();

        BeanUtils.copyProperties(categoryDTO, category);

        category.setStatus(1);

        this.save(category);
    }

    /**
     * 分页查询分类
     * 根据名称模糊查询，按排序字段升序排列
     *
     * @param categoryPageQueryDTO 分页查询条件，包含页码、每页记录数、名称、类型
     * @return 分页结果，包含总记录数和当前页数据
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 创建 Page 对象，设置页码和每页记录数
        Page<Category> page = new Page<>(
                categoryPageQueryDTO.getPage(),
                categoryPageQueryDTO.getPageSize()
        );

        // 创建查询条件
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();

        // 如果有名称条件，添加模糊查询
        if (StringUtils.isNotBlank(categoryPageQueryDTO.getName())) {
            wrapper.like(Category::getName, categoryPageQueryDTO.getName());
        }

        // 如果有类型条件，添加精确查询
        if (categoryPageQueryDTO.getType() != null) {
            wrapper.eq(Category::getType, categoryPageQueryDTO.getType());
        }

        // 按排序字段升序排列
        wrapper.orderByAsc(Category::getSort);

        // 执行分页查询
        Page<Category> resultPage = this.page(page, wrapper);

        // 封装分页结果
        return new PageResult(
                resultPage.getTotal(),  // 总记录数
                resultPage.getRecords() // 当前页数据
        );
    }

    /**
     * 根据 ID 查询分类
     * 将查询结果转换为 CategoryVO 返回
     *
     * @param id 分类 ID
     * @return 分类视图对象
     */
    public CategoryVO getById(Long id) {
        // 根据 ID 查询分类
        Category category = super.getById(id);

        // 将 Category 转换为 CategoryVO
        return CategoryVO.builder()
                .id(category.getId())
                .type(category.getType())
                .name(category.getName())
                .sort(category.getSort())
                .status(category.getStatus())
                .createTime(category.getCreateTime())
                .updateTime(category.getUpdateTime())
                .createUser(category.getCreateUser())
                .updateUser(category.getUpdateUser())
                .build();
    }

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();

        BeanUtils.copyProperties(categoryDTO, category);

        this.updateById(category);
    }

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        Category category = new Category();

        category.setId(id);
        category.setStatus(status);

        this.updateById(category);
    }

    @Override
    @Cacheable(value = "category", key = "'type:' + (#type != null ? #type : 'all')")
    public List<Category> list(Integer type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();

        if (type != null) {
            wrapper.eq(Category::getType, type);
        }

        wrapper.orderByAsc(Category::getSort);

        return this.list(wrapper);
    }

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public void deleteById(Long id) {
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.eq(Dish::getCategoryId, id);
        Long dishCount = (long) dishMapper.selectCount(dishWrapper);

        if (dishCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper<>();
        setmealWrapper.eq(Setmeal::getCategoryId, id);
        Long setmealCount = (long) setmealMapper.selectCount(setmealWrapper);

        if (setmealCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        this.removeById(id);
    }

}
