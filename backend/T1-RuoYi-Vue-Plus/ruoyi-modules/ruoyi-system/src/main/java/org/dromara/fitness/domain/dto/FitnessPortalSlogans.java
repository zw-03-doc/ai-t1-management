package org.dromara.fitness.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** The entrance page has four text positions. The first includes a kicker and a two-line title. */
public record FitnessPortalSlogans(
    @NotBlank(message = "请填写标题上方的小字") @Size(max = 32, message = "标题上方小字最多32个字符") String eyebrow,
    @NotBlank(message = "请填写第一行标题") @Size(max = 12, message = "第一行标题最多12个字符") String titleLead,
    @NotNull(message = "不需要强调文字时请填写空串") @Size(max = 8, message = "强调文字最多8个字符") String titleAccent,
    @NotBlank(message = "请填写第二行标题") @Size(max = 16, message = "第二行标题最多16个字符") String titleSecondLine,
    @NotBlank(message = "请填写副标题") @Size(max = 60, message = "副标题最多60个字符") String subtitle,
    @NotBlank(message = "请填写图片上方文案") @Size(max = 40, message = "图片上方文案最多40个字符") String galleryCaption,
    @NotBlank(message = "请填写底部标语") @Size(max = 40, message = "底部标语最多40个字符") String footerCaption,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NotNull(message = "请先载入当前版本") @PositiveOrZero @Max(2147483646) Integer version
) {
    public FitnessPortalSlogans {
        eyebrow = trim(eyebrow);
        titleLead = trim(titleLead);
        titleAccent = trim(titleAccent);
        titleSecondLine = trim(titleSecondLine);
        subtitle = trim(subtitle);
        galleryCaption = trim(galleryCaption);
        footerCaption = trim(footerCaption);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }

    public static FitnessPortalSlogans defaults() {
        return new FitnessPortalSlogans("YOUR NEXT REP", "为下一次", "进步", "而练。",
            "约一节好课，认真完成每一组。", "在 T1，找到你的训练节奏", "T1 · 每一组，都算数", 0);
    }

    public FitnessPortalSlogans withVersion(Integer next) {
        return new FitnessPortalSlogans(eyebrow, titleLead, titleAccent, titleSecondLine, subtitle, galleryCaption, footerCaption, next);
    }
}
