package org.bok.common.logsfilter.capsule;

import static org.bok.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.bok.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.bok.common.logsfilter.EventPluginLoader;
import org.bok.common.logsfilter.trigger.InternalTransactionPojo;
import org.bok.common.logsfilter.trigger.TransactionLogTrigger;
import org.bok.common.runtime.InternalTransaction;
import org.bok.common.runtime.ProgramResult;
import org.bok.common.utils.StringUtil;
import org.bok.core.Wallet;
import org.bok.core.capsule.BlockCapsule;
import org.bok.core.capsule.TransactionCapsule;
import org.bok.core.db.TransactionTrace;
import org.bok.protos.Protocol;
import org.bok.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.bok.protos.contract.BalanceContract.TransferContract;

@Slf4j
public class TransactionLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private TransactionLogTrigger transactionLogTrigger;

  public TransactionLogTriggerCapsule(TransactionCapsule trxCasule, BlockCapsule blockCapsule) {
    transactionLogTrigger = new TransactionLogTrigger();
    if (Objects.nonNull(blockCapsule)) {
      transactionLogTrigger.setBlockHash(blockCapsule.getBlockId().toString());
    }
    transactionLogTrigger.setTransactionId(trxCasule.getTransactionId().toString());
    transactionLogTrigger.setTimeStamp(blockCapsule.getTimeStamp());
    transactionLogTrigger.setBlockNumber(trxCasule.getBlockNum());

    TransactionTrace trxTrace = trxCasule.getUnTrace();

    //result
    if (Objects.nonNull(trxCasule.getContractRet())) {
      transactionLogTrigger.setResult(trxCasule.getContractRet().toString());
    }

    if (Objects.nonNull(trxCasule.getInstance().getRawData())) {
      // fee limit
      transactionLogTrigger.setFeeLimit(trxCasule.getInstance().getRawData().getFeeLimit());

      Protocol.Transaction.Contract contract = trxCasule.getInstance().getRawData().getContract(0);
      Any contractParameter = null;
      // contract type
      if (Objects.nonNull(contract)) {
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)) {
          transactionLogTrigger.setContractType(contractType.toString());
        }

        contractParameter = contract.getParameter();

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
        try {
          if (contract.getType() == TransferContract) {
            TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);

            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("trx");

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil
                    .encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(
                    StringUtil.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }

              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }

          } else if (contract.getType() == TransferAssetContract) {
            TransferAssetContract contractTransfer = contractParameter
                .unpack(TransferAssetContract.class);

            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getAssetName())) {
                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
              }

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                    StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(StringUtil
                    .encode58Check(contractTransfer.getToAddress().toByteArray()));
              }
              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
          }
        } catch (Exception e) {
          logger.error("failed to load transferAssetContract, error'{}'", e);
        }
      }
    }

    // receipt
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
      transactionLogTrigger.setEnergyFee(trxTrace.getReceipt().getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(trxTrace.getReceipt().getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(trxTrace.getReceipt().getEnergyUsageTotal());
      transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
      transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
      transactionLogTrigger.setEnergyUsage(trxTrace.getReceipt().getEnergyUsage());
    }

    // program result
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getRuntime()) && Objects
        .nonNull(trxTrace.getRuntime().getResult())) {
      ProgramResult programResult = trxTrace.getRuntime().getResult();
      ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
      ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

      if (Objects.nonNull(contractResult) && contractResult.size() > 0) {
        transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0) {
        transactionLogTrigger
            .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
      }

      // internal transaction
      transactionLogTrigger.setInternalTrananctionList(
          getInternalTransactionList(programResult.getInternalTransactions()));
    }
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    transactionLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  private List<InternalTransactionPojo> getInternalTransactionList(
      List<InternalTransaction> internalTransactionList) {
    List<InternalTransactionPojo> pojoList = new ArrayList<>();

    internalTransactionList.forEach(internalTransaction -> {
      InternalTransactionPojo item = new InternalTransactionPojo();

      item.setHash(Hex.toHexString(internalTransaction.getHash()));
      item.setCallValue(internalTransaction.getValue());
      item.setTokenInfo(internalTransaction.getTokenInfo());
      item.setCaller_address(Hex.toHexString(internalTransaction.getSender()));
      item.setTransferTo_address(Hex.toHexString(internalTransaction.getTransferToAddress()));
      item.setData(Hex.toHexString(internalTransaction.getData()));
      item.setRejected(internalTransaction.isRejected());
      item.setNote(internalTransaction.getNote());

      pojoList.add(item);
    });

    return pojoList;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
