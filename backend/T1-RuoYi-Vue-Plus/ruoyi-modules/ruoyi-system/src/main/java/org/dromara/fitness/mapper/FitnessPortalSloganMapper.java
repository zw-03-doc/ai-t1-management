package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.fitness.domain.dto.FitnessPortalSlogans;

/** Each query binds a tenant chosen by the server, including anonymous reads for the T1 mini-program. */
@InterceptorIgnore(tenantLine = "true")
public interface FitnessPortalSloganMapper {
    @Select("""
        SELECT eyebrow, title_lead AS titleLead, title_accent AS titleAccent, title_second_line AS titleSecondLine,
               subtitle, gallery_caption AS galleryCaption, footer_caption AS footerCaption, version
        FROM fit_portal_slogans WHERE tenant_id = #{tenant}
        """)
    FitnessPortalSlogans find(@Param("tenant") String tenant);

    @Insert("""
        INSERT INTO fit_portal_slogans (tenant_id, eyebrow, title_lead, title_accent, title_second_line,
            subtitle, gallery_caption, footer_caption, version)
        VALUES (#{tenant}, #{s.eyebrow}, #{s.titleLead}, #{s.titleAccent}, #{s.titleSecondLine},
            #{s.subtitle}, #{s.galleryCaption}, #{s.footerCaption}, 1)
        """)
    int insert(@Param("tenant") String tenant, @Param("s") FitnessPortalSlogans slogans);

    @Update("""
        UPDATE fit_portal_slogans SET eyebrow=#{s.eyebrow}, title_lead=#{s.titleLead}, title_accent=#{s.titleAccent},
            title_second_line=#{s.titleSecondLine}, subtitle=#{s.subtitle}, gallery_caption=#{s.galleryCaption},
            footer_caption=#{s.footerCaption}, version=version+1
        WHERE tenant_id=#{tenant} AND version=#{s.version}
        """)
    int updateVersion(@Param("tenant") String tenant, @Param("s") FitnessPortalSlogans slogans);
}
