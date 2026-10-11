package org.dromara.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitCourse;
import org.dromara.fitness.domain.bo.FitnessCourseBo;
import org.dromara.fitness.domain.vo.FitnessCourseVo;
import org.dromara.fitness.mapper.FitnessCourseMapper;
import org.dromara.fitness.service.IFitnessCourseService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 课程 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessCourseServiceImpl implements IFitnessCourseService {

    private final FitnessCourseMapper baseMapper;

    @Override
    public FitnessCourseVo queryById(Long courseId) {
        return baseMapper.selectCourseById(courseId);
    }

    @Override
    public TableDataInfo<FitnessCourseVo> queryPageList(FitnessCourseBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageCourseList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessCourseVo> queryList(FitnessCourseBo bo) {
        return baseMapper.selectCourseList(bo);
    }

    @Override
    public Boolean insertByBo(FitnessCourseBo bo) {
        FitCourse add = MapstructUtils.convert(bo, FitCourse.class);
        add.setCourseId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        add.setCourseNo(FitnessBizCodeUtils.defaultIfBlank(bo.getCourseNo(), "CRS"));
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setCourseId(add.getCourseId());
            bo.setCourseNo(add.getCourseNo());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(FitnessCourseBo bo) {
        FitCourse db = baseMapper.selectById(bo.getCourseId());
        if (db == null) {
            throw new ServiceException("课程不存在");
        }
        FitCourse update = MapstructUtils.convert(bo, FitCourse.class);
        update.setCourseNo(FitnessBizCodeUtils.fillOrKeep(bo.getCourseNo(), db.getCourseNo(), "CRS"));
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }


    @Override
    public Boolean updateStatus(Long courseId, String status) {
        FitCourse update = new FitCourse();
        update.setCourseId(courseId);
        update.setStatus(status);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourse>().in(FitCourse::getCourseId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分数据不存在或已被删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void validEntityBeforeSave(FitCourse entity) {
        if (StringUtils.isNotBlank(entity.getCourseNo()) && baseMapper.exists(new LambdaQueryWrapper<FitCourse>()
            .eq(FitCourse::getCourseNo, entity.getCourseNo())
            .ne(entity.getCourseId() != null, FitCourse::getCourseId, entity.getCourseId()))) {
            throw new ServiceException("课程编号已存在");
        }
    }


    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }

}
