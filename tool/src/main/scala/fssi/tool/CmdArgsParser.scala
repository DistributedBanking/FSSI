package fssi
package tool

import scopt._
import CmdArgs._
import types._
import interpreter.jsonCodecs._

object CmdArgsParser extends OptionParser[CmdArgs]("fssitool") {

  head("fssi_tool", "0.1.0")
  help("help").abbr("h").text("print this help messages")

  cmd("CreateAccount")
    .action((_, _) => CreateAccountArgs())
    .text("Create An FSSI Account")
    .children(
      opt[String]("password")
        .abbr("p")
        .required()
        .action((x, c) => c.asInstanceOf[CreateAccountArgs].copy(password = x))
    )

  cmd("CreateChain")
    .action((_, _) => CreateChainArgs(new java.io.File("."), "testnet"))
    .text("Create a chain")
    .children(
      opt[java.io.File]("data-dir")
        .abbr("d")
        .required()
        .action((x, c) => c.asInstanceOf[CreateChainArgs].copy(dataDir = x)),
      opt[String]("chain-id")
        .abbr("id")
        .required()
        .action((x, c) => c.asInstanceOf[CreateChainArgs].copy(chainID = x))
    )

  cmd("CompileContract")
    .text("Compile Smart Contract Project")
    .action((_, _) => CompileContractArgs())
    .children(
      opt[java.io.File]("project-directory")
        .abbr("pd")
        .text("smart contract project root path")
        .required()
        .action((x, c) => c.asInstanceOf[CompileContractArgs].copy(projectDirectory = x)),
      opt[java.io.File]("output-file")
        .abbr("of")
        .text("the compiled artifact file name, with absolute path")
        .required()
        .action((x, c) => c.asInstanceOf[CompileContractArgs].copy(outputFile = x)),
      opt[String]("sandbox-version")
        .abbr("sv")
        .text("supported version of the sandbox on which the smart contract will run, default is 1.0.0(only support 1.0.0 now)")
        .action((x, c) => c.asInstanceOf[CompileContractArgs].copy(sandboxVersion = CompileContractArgs.SandobxVersion(x)))
    )

  cmd("CreateTransaction")
    .text("Create Transaction")
    .children(
      cmd("transfer")
        .text("create transfer transaction")
        .action((_, _) => CreateTransferTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("payer account file created by 'CreateAccount'")
            .action((f, c) => c.asInstanceOf[CreateTransferTransactionArgs].copy(accountFile = f)),
          opt[String]("password")
            .abbr("p")
            .required()
            .text("payer's account password")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs]
                .copy(password = x.getBytes("utf-8"))),
          opt[String]("payee-id")
            .abbr("pi")
            .required()
            .text("payee's account id, the hex string of it's public key")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs]
                .copy(payee = Account.ID(HexString.decode(x)))),
          opt[String]("token")
            .abbr("t")
            .required()
            .text("amount to be transfered, in form of 'number' + 'unit', eg. 100Sweet. ")
            .action((x, c) =>
              c.asInstanceOf[CreateTransferTransactionArgs].copy(token = Token.parse(x)))
        ),
      cmd("publishContract")
        .text("create publish contract transaction")
        .action((_, _) => CreatePublishContractTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("payer account file created by 'CreateAccount'")
            .action((f, c) =>
              c.asInstanceOf[CreatePublishContractTransactionArgs].copy(accountFile = f)),
          opt[String]("password")
            .abbr("p")
            .required()
            .text("payer's account password")
            .action((x, c) =>
              c.asInstanceOf[CreatePublishContractTransactionArgs]
                .copy(password = x.getBytes("utf-8"))),
          opt[java.io.File]("contract-file")
            .abbr("cf")
            .required()
            .text("smart contract file")
            .action((x, c) =>
              c.asInstanceOf[CreatePublishContractTransactionArgs].copy(contractFile = x)),
          opt[String]("contract-name")
            .abbr("name")
            .required()
            .text("the uniquename of the contract, eg. com.blabla.finance")
            .action((x, c) =>
              c.asInstanceOf[CreatePublishContractTransactionArgs]
                .copy(contractName = UniqueName(x))),
          opt[String]("contract-version")
            .abbr("version")
            .required()
            .text("the version of the contract")
            .action((x, c) =>
              c.asInstanceOf[CreatePublishContractTransactionArgs]
                .copy(contractVersion = Version(x)))
        ),
      cmd("runContract")
        .text("create run contract transaction")
        .action((_, _) => CreateRunContractTransactionArgs())
        .children(
          opt[java.io.File]("account-file")
            .abbr("af")
            .required()
            .text("invoker account file created by 'CreateAccount'")
            .action(
              (f, c) => c.asInstanceOf[CreateRunContractTransactionArgs].copy(accountFile = f)),
          opt[String]("password")
            .abbr("p")
            .required()
            .text("invoker's account password")
            .action((x, c) =>
              c.asInstanceOf[CreateRunContractTransactionArgs]
                .copy(password = x.getBytes("utf-8"))),
          opt[String]("contract-name")
            .abbr("name")
            .required()
            .text("invoking contract name")
            .action((x, c) =>
              c.asInstanceOf[CreateRunContractTransactionArgs].copy(contractName = UniqueName(x))),
          opt[String]("contract-version")
            .abbr("version")
            .required()
            .text("the version of the invoking contract")
            .action((x, c) =>
              c.asInstanceOf[CreateRunContractTransactionArgs]
                .copy(contractVersion = Version(x))),
          opt[String]("method")
            .abbr("m")
            .text("method to invoke")
            .action((x, c) => c.asInstanceOf[CreateRunContractTransactionArgs].copy(method = Contract.Method(x))),
          opt[String]("parameter")
            .abbr("p")
            .text("parameters for this invoking")
            .action((x, c) =>
              c.asInstanceOf[CreateRunContractTransactionArgs]
                .copy(parameter = io.circe.parser.parse(x).right.get.as[Contract.Parameter].right.get))
        )
    )

}
