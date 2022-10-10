/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AssetClientTest extends AbstractContainerTest {

    @Test
    public void testAssets() throws Exception {
        // create asset and assign to edge
        Asset savedAsset = saveAndAssignAssetToEdge();
        cloudRestClient.assignAssetToEdge(edge.getId(), savedAsset.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset.getId()).isPresent());

        // update asset
        savedAsset.setName("Updated Asset Name");
        cloudRestClient.saveAsset(savedAsset);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Asset Name".equals(edgeRestClient.getAssetById(savedAsset.getId()).get().getName()));

        // save asset attribute
        JsonNode assetAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"assetKey\":\"assetValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedAsset.getId(), DataConstants.SERVER_SCOPE, assetAttributes);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedAsset.getId(),
                        DataConstants.SERVER_SCOPE, "assetKey", "assetValue"));

        // assign asset to customer
        Customer customer = new Customer();
        customer.setTitle("Asset Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        cloudRestClient.assignAssetToCustomer(savedCustomer.getId(), savedAsset.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getAssetById(savedAsset.getId()).get().getCustomerId()));

        // unassign asset from customer
        cloudRestClient.unassignAssetFromCustomer(savedAsset.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getAssetById(savedAsset.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // unassign asset from edge
        cloudRestClient.unassignAssetFromEdge(edge.getId(), savedAsset.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset.getId()).isEmpty());
        cloudRestClient.deleteAsset(savedAsset.getId());
    }

}
