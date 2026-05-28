package com.vr.common.annotation;

import com.vr.common.enums.BusinessType;
import com.vr.common.enums.OperatorType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String title() default "";
    BusinessType businessType() default BusinessType.OTHER;
    OperatorType operatorType() default OperatorType.MANAGE;
}
