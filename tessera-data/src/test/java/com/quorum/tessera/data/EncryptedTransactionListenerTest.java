package com.quorum.tessera.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.quorum.tessera.enclave.EncodedPayload;
import com.quorum.tessera.enclave.EncodedPayloadCodec;
import com.quorum.tessera.enclave.PayloadEncoder;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class EncryptedTransactionListenerTest {

  private final MockedStatic<PayloadEncoder> payloadEncoderFactoryFunction =
      mockStatic(PayloadEncoder.class);

  private EncryptedTransactionListener encryptedTransactionListener;

  private PayloadEncoder payloadEncoder;

  @Before
  public void beforeTest() {
    encryptedTransactionListener = new EncryptedTransactionListener();
    payloadEncoder = mock(PayloadEncoder.class);
    payloadEncoderFactoryFunction
        .when(() -> PayloadEncoder.create(EncodedPayloadCodec.UNSUPPORTED))
        .thenReturn(Optional.of(payloadEncoder));
  }

  @After
  public void afterTest() {
    try {
      verifyNoMoreInteractions(payloadEncoder);
      payloadEncoderFactoryFunction.verifyNoMoreInteractions();
    } finally {
      payloadEncoderFactoryFunction.close();
    }
  }

  @Test
  public void onLoad() {

    byte[] payloadData = "PayloadData".getBytes();

    EncryptedTransaction encryptedTransaction = new EncryptedTransaction();
    encryptedTransaction.setEncodedPayloadCodec(EncodedPayloadCodec.UNSUPPORTED);
    encryptedTransaction.setEncodedPayload(payloadData);

    encryptedTransactionListener.onLoad(encryptedTransaction);

    verify(payloadEncoder).decode(payloadData);

    payloadEncoderFactoryFunction.verify(
        () -> PayloadEncoder.create(EncodedPayloadCodec.UNSUPPORTED));
  }

  @Test
  public void onSaveNullPayload() {
    EncryptedTransaction encryptedTransaction = new EncryptedTransaction();
    encryptedTransactionListener.onSave(encryptedTransaction);
  }

  @Test
  public void onSave() {
    EncodedPayload encodedPayload = mock(EncodedPayload.class);
    EncryptedTransaction encryptedTransaction = new EncryptedTransaction();
    encryptedTransaction.setPayload(encodedPayload);
    encryptedTransaction.setEncodedPayloadCodec(EncodedPayloadCodec.UNSUPPORTED);

    byte[] payloadData = "PayloadData".getBytes();
    when(payloadEncoder.encode(encodedPayload)).thenReturn(payloadData);

    encryptedTransactionListener.onSave(encryptedTransaction);

    verify(payloadEncoder).encode(encodedPayload);
    payloadEncoderFactoryFunction.verify(
        () -> PayloadEncoder.create(EncodedPayloadCodec.UNSUPPORTED));
    assertThat(encryptedTransaction.getEncodedPayload()).isEqualTo(payloadData);
  }
}
