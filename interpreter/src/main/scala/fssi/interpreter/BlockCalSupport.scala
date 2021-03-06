package fssi
package interpreter

import utils._
import types._, implicits._
import ast._

/** calclute Block bytes/hash...
  */
trait BlockCalSupport {

  /** calculate the hash(sha3-256) of a block
    * then, fill the `hash` field to return the new block.
    * the participating fileds include previousHash, height, transactions and chainID
    */
  private[interpreter] def hashBlock(block: Block): Block = {
    val allBytes = block.previousHash.bytes ++
      block.previousTokenState.bytes ++
      block.previousContractState.bytes ++
      block.previousContractDataState.bytes ++
      block.height.toByteArray ++ block.transactions
      .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesToHashForTransaction(n)) ++ block.chainID
      .getBytes("utf-8")
    val hashBytes = cryptoUtil.hash(allBytes)
    block.copy(hash = Hash(hashBytes))
  }

  /** calculate a block bytes, included hash */
  private[interpreter] def calculateTotalBlockBytes(block: Block): Array[Byte] = {
    val allBytes = block.previousHash.bytes ++
      block.previousTokenState.bytes ++
      block.previousContractState.bytes ++
      block.previousContractDataState.bytes ++
      block.height.toByteArray ++
      block.transactions
        .foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesToHashForTransaction(n)) ++
      block.chainID.getBytes("utf-8") ++
      block.hash.bytes

    allBytes
  }

  /** calclute transaction's bytes to be signed */
  private[interpreter] def calculateBytesToBeSignedOfTransaction(
      transaction: Transaction): BytesValue = transaction match {
    case transfer: Transaction.Transfer =>
      // id, payer, payee, token, timestamp
      BytesValue(transfer.id.value.getBytes("utf-8")) ++
        transfer.payer.value.toBytesValue ++ transfer.payee.value.toBytesValue ++
        BytesValue(transfer.token.toString.getBytes("utf-8")) ++
        BytesValue(BigInt(transfer.timestamp).toByteArray)

    case publishContract: Transaction.PublishContract =>
      BytesValue(publishContract.id.value.getBytes("utf-8")) ++
        publishContract.owner.value.toBytesValue ++
        calculateBytesToBeSignedOfUserContract(publishContract.contract) ++
        publishContract.contract.signature.value.toBytesValue ++
        BytesValue(BigInt(publishContract.timestamp).toByteArray)

    case runContract: Transaction.RunContract =>
      import Contract.Parameter._
      def parametersToBytesValue(parameter: Contract.Parameter): BytesValue = parameter match {
        case PEmpty         => BytesValue.empty
        case PString(value) => BytesValue(value.getBytes("utf-8"))
        case PBigDecimal(value) =>
          BytesValue(value.unscaledValue.toByteArray) ++ BytesValue(BigInt(value.scale).toByteArray)
        case PBool(value) => if (value) BytesValue(Array(1.toByte)) else BytesValue(Array(0.toByte))
        case PArray(array) =>
          array.foldLeft(BytesValue.empty)((acc, n) => acc ++ parametersToBytesValue(n))
      }

      BytesValue(runContract.id.value.getBytes("utf-8")) ++
        runContract.sender.value.toBytesValue ++
        BytesValue(runContract.contractName.value.getBytes("utf-8")) ++
        BytesValue(runContract.contractVersion.value.getBytes("utf-8")) ++
        BytesValue(runContract.contractMethod.toString.getBytes("utf-8")) ++
        parametersToBytesValue(runContract.contractParameter) ++
        BytesValue(BigInt(runContract.timestamp).toByteArray)
  }

  /** serialize transaction to byte array.
    * we should guarantee the serialization is determined,
    * for the same transaction, we should get the same byte array.
    */
  private def bytesToHashForTransaction(transaction: Transaction): Array[Byte] = transaction match {
    case x: Transaction.Transfer =>
      x.id.value.getBytes("utf-8") ++
        x.payer.value.bytes ++
        x.payee.value.bytes ++
        x.token.amount.toByteArray ++ x.token.tokenUnit.toString.getBytes("utf-8") ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray

    case x: Transaction.PublishContract =>
      x.id.value.getBytes("utf-8") ++
        x.owner.value.bytes ++
        x.contract.owner.value.bytes ++
        x.contract.name.value.getBytes("utf-8") ++
        x.contract.version.value.getBytes("utf-8") ++
        x.contract.meta.methods.foldLeft(Array.emptyByteArray)((acc, n) =>
          acc ++ n.toString.getBytes("utf-8")) ++
        x.contract.signature.value.bytes ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray

    case x: Transaction.RunContract =>
      import Contract.Parameter._
      def bytesOfParam(p: Contract.Parameter): Array[Byte] = p match {
        case PEmpty         => Array.emptyByteArray
        case PString(x)     => x.getBytes("utf-8")
        case PBigDecimal(x) => x.toString.getBytes("utf-8")
        case PBool(x)       => BigInt(if (x) 1 else 0).toByteArray
        case PArray(array) =>
          array.foldLeft(Array.emptyByteArray)((acc, n) => acc ++ bytesOfParam(n))
      }

      x.id.value.getBytes("utf-8") ++
        x.sender.value.bytes ++
        x.contractName.value.getBytes("utf-8") ++
        x.contractVersion.value.getBytes("utf-8") ++
        bytesOfParam(x.contractParameter) ++
        x.signature.value.bytes ++
        BigInt(x.timestamp).toByteArray
  }

  private [interpreter] def calculateBytesToBeSignedOfUserContract(contract:Contract.UserContract):BytesValue={
    contract.owner.value.toBytesValue ++
      BytesValue(contract.name.value.getBytes("utf-8")) ++
      BytesValue(contract.version.value.getBytes("utf-8")) ++
      contract.code.toBytesValue ++
      contract.meta.methods.foldLeft(BytesValue.empty)((acc, n) =>
        acc ++ BytesValue(n.toString().getBytes("utf-8")))
  }

}
