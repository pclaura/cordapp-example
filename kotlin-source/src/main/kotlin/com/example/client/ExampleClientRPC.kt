package com.example.client

import com.example.state.IOUState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    ExampleClientRPC().main(args) //Llamada al main de la clase 'ExampleClientRPC()'
}

private class ExampleClientRPC {
    companion object {
        val logger: Logger = loggerFor<ExampleClientRPC>() //Variable de la clase Logger (loggerFor<ExampleClientRPC>)
        private fun logState(state: StateAndRef<IOUState>) = logger.info("{}", state.state.data) //FUNCTION EXPRESSION!
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ExampleClientRPC <node address>" } //TODO: PREGUNTAR {}
        val nodeAddress = NetworkHostAndPort.parse(args[0]) //Direccion del cliente que se le pasa como argumento
        val client = CordaRPCClient(nodeAddress) //Instancia un cliente

        // Can be amended in the com.example.MainKt file.
        val proxy = client.start("user1", "test").proxy //Crea proxy para invocar RPCs en el servidor

        // Grab all existing and future IOU states in the vault.
        val (snapshot, updates) = proxy.vaultTrack(IOUState::class.java) //Devuelve las actualizaciones del vault. Notifica cada vez que un nuevo state se guarda en el vault

        // Log the 'placed' IOU states and listen for new ones.
        snapshot.states.forEach { logState(it) } //Las ofertas lanzadas se almacenan
        updates.toBlocking().subscribe { update -> //Manejo de las actualizaciones
            update.produced.forEach { logState(it) }
        }
    }
}
