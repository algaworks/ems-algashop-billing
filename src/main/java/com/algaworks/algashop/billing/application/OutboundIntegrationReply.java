package com.algaworks.algashop.billing.application;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface OutboundIntegrationReply {
	@JsonIgnore
	String getAggregateId();
}
