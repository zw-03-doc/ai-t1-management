package org.dromara.fitness.mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.FitMember;
import java.util.List;
public interface FitnessIdentityMapper {
    FitMember lockMember(@Param("tenant") String tenant, @Param("memberId") Long memberId);
    List<FitMember> lockPhoneOwners(@Param("tenant") String tenant, @Param("mobile") String mobile);
    long countMemberBusiness(@Param("tenant") String tenant, @Param("memberId") Long memberId);
    int retireBlankIdentity(@Param("tenant") String tenant, @Param("memberId") Long memberId);
    int claimIdentity(@Param("tenant") String tenant, @Param("memberId") Long memberId, @Param("openid") String openid, @Param("unionid") String unionid);
    long countPrivilegedCoachAccount(@Param("userId") Long userId);
    long countCoachReferences(@Param("tenant") String tenant, @Param("coachId") Long coachId);
}
