package com.quorum.tessera.test.rest;

import static com.quorum.tessera.version.MandatoryRecipientsVersion.MIME_TYPE_JSON_4;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.quorum.tessera.api.ReceiveResponse;
import com.quorum.tessera.api.SendRequest;
import com.quorum.tessera.api.SendResponse;
import com.quorum.tessera.test.Party;
import com.quorum.tessera.test.PartyHelper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Test;
import suite.NodeAlias;

public class SendReceiveMandatoryRecipientsIT {

  private final PartyHelper partyHelper = PartyHelper.create();

  private RestUtils utils = new RestUtils();

  final Party a = partyHelper.findByAlias(NodeAlias.A);
  final Party b = partyHelper.findByAlias(NodeAlias.B);
  final Party c = partyHelper.findByAlias(NodeAlias.C);

  @Test
  public void invalidRequests() {

    byte[] transactionData = utils.createTransactionData();

    final SendRequest sendRequest = new SendRequest();
    sendRequest.setPayload(transactionData);
    sendRequest.setTo(b.getPublicKey());
    sendRequest.setPrivacyFlag(2);
    sendRequest.setMandatoryRecipients(c.getPublicKey());

    final Response response =
      a.getRestClient()
        .target(a.getQ2TUri())
        .path("/send")
        .request()
        .post(Entity.entity(sendRequest, MIME_TYPE_JSON_4));

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(403);

  }

  @Test
  public void sendTransactionWithMandatoryRecipients() throws UnsupportedEncodingException {

    byte[] transactionData = utils.createTransactionData();

    final SendRequest sendRequest = new SendRequest();
    sendRequest.setPayload(transactionData);
    sendRequest.setTo(b.getPublicKey(), c.getPublicKey());
    sendRequest.setPrivacyFlag(2);
    sendRequest.setMandatoryRecipients(c.getPublicKey());

    final Response response =
        a.getRestClient()
            .target(a.getQ2TUri())
            .path("/send")
            .request()
            .post(Entity.entity(sendRequest, MIME_TYPE_JSON_4));

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(201);

    final SendResponse result = response.readEntity(SendResponse.class);
    final String hash = result.getKey();

    // Hash length should be 64 bytes
    assertThat(Base64.getDecoder().decode(hash)).hasSize(64);

    final String encodedHash = URLEncoder.encode(hash, UTF_8.toString());
    assertThat(hash).isNotNull().isNotBlank();

    final Response receiveResponse =
        a.getRestClient()
            .target(a.getQ2TUri())
            .path("/transaction")
            .path(encodedHash)
            .request()
            .accept(MIME_TYPE_JSON_4)
            .buildGet()
            .invoke();

    // validate result
    assertThat(receiveResponse).isNotNull();
    assertThat(receiveResponse.getStatus()).isEqualTo(200);

    final ReceiveResponse receiveResult = receiveResponse.readEntity(ReceiveResponse.class);

    assertThat(receiveResult.getPayload()).isEqualTo(transactionData);
    assertThat(receiveResult.getManagedParties()).containsExactly(a.getPublicKey());
    assertThat(receiveResult.getSenderKey()).isEqualTo(a.getPublicKey());
    assertThat(receiveResult.getMandatoryRecipients()).containsExactly(c.getPublicKey());

    final Response receiveResponseOnC =
        c.getRestClient()
            .target(c.getQ2TUri())
            .path("/transaction")
            .path(encodedHash)
            .request()
            .accept(MIME_TYPE_JSON_4)
            .buildGet()
            .invoke();

    // validate result
    assertThat(receiveResponseOnC).isNotNull();
    assertThat(receiveResponseOnC.getStatus()).isEqualTo(200);

    final ReceiveResponse receiveResultOnC = receiveResponseOnC.readEntity(ReceiveResponse.class);
    assertThat(receiveResultOnC.getPayload()).isEqualTo(transactionData);
    assertThat(receiveResultOnC.getManagedParties()).containsExactly(c.getPublicKey());
    assertThat(receiveResultOnC.getSenderKey()).isEqualTo(a.getPublicKey());
    assertThat(receiveResultOnC.getMandatoryRecipients()).containsExactly(c.getPublicKey());
  }
}
