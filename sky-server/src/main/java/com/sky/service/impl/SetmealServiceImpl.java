package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.SetmealDishService;
import com.sky.service.SetmealService;
import com.sky.vo.CategoryVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "setmeal", allEntries = true)
    public void save(SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        this.save(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmeal.getId());
                setmealDishService.save(setmealDish);
            }
        }

        log.info("新增套餐成功，id: {}", setmeal.getId());
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐：{}", setmealPageQueryDTO);

        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        Page<Setmeal> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();

        if (setmealPageQueryDTO.getName() != null && !setmealPageQueryDTO.getName().isEmpty()) {
            wrapper.like(Setmeal::getName, setmealPageQueryDTO.getName());
        }

        if (setmealPageQueryDTO.getCategoryId() != null) {
            wrapper.eq(Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId());
        }

        if (setmealPageQueryDTO.getStatus() != null) {
            wrapper.eq(Setmeal::getStatus, setmealPageQueryDTO.getStatus());
        }

        Page<Setmeal> resultPage = this.page(page, wrapper);

        List<SetmealVO> setmealVOList = new ArrayList<>();
        for (Setmeal setmeal : resultPage.getRecords()) {
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);

            CategoryVO categoryVO = categoryService.getById(setmeal.getCategoryId());
            if (categoryVO != null) {
                setmealVO.setCategoryName(categoryVO.getName());
            }

            List<SetmealDish> setmealDishes = setmealDishService.getBySetmealId(setmeal.getId());
            setmealVO.setSetmealDishes(setmealDishes);

            setmealVOList.add(setmealVO);
        }

        return new PageResult(resultPage.getTotal(), setmealVOList);
    }

    @Override
    public SetmealVO getWithDishes(Long id) {
        log.info("根据 ID 查询套餐，id: {}", id);

        Setmeal setmeal = this.getById(id);
        if (setmeal == null) {
            throw new RuntimeException("套餐不存在");
        }

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        CategoryVO categoryVO = categoryService.getById(setmeal.getCategoryId());
        if (categoryVO != null) {
            setmealVO.setCategoryName(categoryVO.getName());
        }

        List<SetmealDish> setmealDishes = setmealDishService.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "setmeal", allEntries = true)
    public void update(SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        this.updateById(setmeal);

        setmealDishService.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealDTO.getId());
                setmealDishService.save(setmealDish);
            }
        }

        log.info("修改套餐成功，id: {}", setmealDTO.getId());
    }

    @Override
    @CacheEvict(value = "setmeal", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        log.info("启用或禁用套餐，id: {}, status: {}", id, status);

        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);

        this.updateById(setmeal);
    }

    @Override
    @Cacheable(value = "setmeal", key = "'categoryId:' + #categoryId")
    public List<Setmeal> listByCategoryId(Long categoryId) {
        log.info("根据分类 ID 查询套餐，categoryId: {}", categoryId);

        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            wrapper.eq(Setmeal::getCategoryId, categoryId);
        }

        wrapper.eq(Setmeal::getStatus, 1);

        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "setmeal", allEntries = true)
    public void deleteById(Long id) {
        log.info("删除套餐，id: {}", id);

        Setmeal setmeal = this.getById(id);
        if (setmeal == null) {
            throw new DeletionNotAllowedException("套餐不存在");
        }

        if (setmeal.getStatus() == 1) {
            throw new DeletionNotAllowedException("正在销售中的套餐不能删除");
        }

        this.removeById(id);

        setmealDishService.deleteBySetmealId(id);

        log.info("删除套餐成功，id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "setmeal", allEntries = true)
    public void deleteByIds(String ids) {
        log.info("批量删除套餐，ids: {}", ids);

        String[] idArray = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String id : idArray) {
            idList.add(Long.parseLong(id));
        }

        for (Long id : idList) {
            deleteById(id);
        }

        log.info("批量删除套餐成功，ids: {}", ids);
    }
}
