/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
@TbCoreComponent
public class TenantEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertTenantEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        TenantId tenantId = new TenantId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        if (EdgeEventActionType.UPDATED.equals(edgeEvent.getAction())) {
            Tenant tenant = tenantService.findTenantById(tenantId);
            if (tenant != null) {
                UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                TenantUpdateMsg tenantUpdateMsg = ((TenantMsgConstructor)
                        tenantMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                        .constructTenantUpdateMsg(msgType, tenant);
                TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenant.getTenantProfileId());
                TenantProfileUpdateMsg tenantProfileUpdateMsg = ((TenantMsgConstructor)
                        tenantMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                        .constructTenantProfileUpdateMsg(msgType, tenantProfile, edgeVersion);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addTenantUpdateMsg(tenantUpdateMsg)
                        .addTenantProfileUpdateMsg(tenantProfileUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }
}
