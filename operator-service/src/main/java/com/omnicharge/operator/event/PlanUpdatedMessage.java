package com.omnicharge.operator.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdatedMessage implements Serializable {
    private String eventId;
    private Long operatorId;
    private Long timestamp;
}
