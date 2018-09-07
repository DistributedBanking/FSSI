package fssi
package interpreter

import contract.lib._
import jsonCodecs._
import utils._
import trie._
import types._
import exception._
import implicits._
import ast._
import java.io._
import java.nio.charset.Charset

import scala.collection._
import io.circe.parser._
import io.circe.syntax._
import Bytes.implicits._
import better.files.{File => ScalaFile, _}
import java.io._
import java.nio.file.Paths
import java.util.UUID

import fssi.sandbox.exception.ContractRunningException

class ContractServiceHandler extends ContractService.Handler[Stack] {

  val sandbox = new fssi.sandbox.SandBox

  /** check the smart contract project to see where it is full-deterministic or not
    */
  override def checkDeterminismOfContractProject(
      rootPath: File): Stack[Either[FSSIException, Unit]] = Stack { setting =>
    sandbox.checkContractDeterminism(rootPath)
  }

  /** compile smart contract project and output to the target file
    */
  override def compileContractProject(rootPath: File,
                                      sandboxVersion: String,
                                      outputFile: File): Stack[Either[FSSIException, Unit]] =
    Stack { setting =>
      sandbox.compileContract(rootPath.toPath, sandboxVersion, outputFile)
    }

  /** create a running context for some transaction
    */
  override def createContextInstance(sqlStore: SqlStore,
                                     kvStore: KVStore,
                                     tokenQuery: TokenQuery): Stack[Context] = Stack { setting =>
    ContractRunningContext(sqlStore, kvStore, tokenQuery)
  }

  override def createUserContractFromContractFile(
      account: Account,
      contractFile: File,
      contractName: UniqueName,
      contractVersion: Version): Stack[Either[FSSIException, Contract.UserContract]] = Stack {
    setting =>
      sandbox.buildContract(account.id, contractFile, contractName, contractVersion)
  }

  override def invokeUserContract(context: Context,
                                  contract: Contract.UserContract,
                                  method: Contract.Method,
                                  params: Contract.Parameter): Stack[Either[Throwable, Unit]] =
    Stack { setting =>
      contract.meta.methods.find(_.alias == method.alias) match {
        case Some(_) =>
          val contractFile =
            Paths
              .get(System.getProperty("user.home"),
                   s".fssi/.${contract.name.value}_${contract.version.value}")
              .toFile
          if (!contractFile.getParentFile.exists()) contractFile.getParentFile.mkdirs()
          if (contractFile.exists()) FileUtil.deleteDir(contractFile.toPath)
          contractFile.createNewFile()
          val fileOutputStream = new FileOutputStream(contractFile, true)
          try {
            fileOutputStream.write(contract.code.bytes, 0, contract.code.bytes.length)
            fileOutputStream.flush()
            sandbox.executeContract(context, contractFile, method, params)
          } catch {
            case t: Throwable => Left(t)
          } finally {
            if (fileOutputStream != null) fileOutputStream.close()
            if (contractFile.exists()) FileUtil.deleteDir(contractFile.toPath)
          }
        case None =>
          Left(ContractRunningException(Vector(
            s"can not find method ${method.alias} in contract ${contract.name.value}#${contract.version.value}")))
      }
    }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = instance
  }
}
