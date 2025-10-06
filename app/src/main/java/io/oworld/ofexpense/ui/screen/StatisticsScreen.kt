package io.oworld.ofexpense.ui.screen

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.oworld.ofexpense.R
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.Category
import io.oworld.ofexpense.db.Expense
import io.oworld.ofexpense.db.Preference
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun StatisticsScreen(
    paddingValues: PaddingValues, viewModel: StatisticsViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    val messageOfServer by viewModel.messageOfServer.collectAsState()
    val messageOfClient by viewModel.messageOfClient.collectAsState()
    val serverMessageScroll = rememberScrollState(0)
    val clientMessageScroll = rememberScrollState(0)
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Button(onClick = { viewModel.syncAsServer() }) {
                    Text(getStr(R.string.sync_as_server), fontSize = 13.sp)
                }
                Text(
                    messageOfServer,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .height(400.dp)
                        .verticalScroll(serverMessageScroll)
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Button(onClick = { viewModel.syncAsClient() }) {
                    Text(getStr(R.string.sync_as_client), fontSize = 13.sp)
                }
                Text(
                    messageOfClient,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .height(400.dp)
                        .verticalScroll(clientMessageScroll)
                )
            }
        }
    }
    LaunchedEffect(scope) { viewModel.initPreference() }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val _messageOfServer = MutableStateFlow("")
    val messageOfServer = _messageOfServer.asStateFlow()
    private val _messageOfClient = MutableStateFlow("")
    val messageOfClient = _messageOfClient.asStateFlow()
    private val resources = appContext.resources

    fun initPreference() = viewModelScope.launch(Dispatchers.IO) {
        if (appDatabase.preferenceDao().get() == null) {
            appDatabase.preferenceDao().upsert(
                Preference(
                    id = 1,
                    syncDateTime = 0L
                )
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun saveNewDb(categoryList: List<Category>, expenseList: List<Expense>) =
        viewModelScope.launch {
            categoryList.forEach { category -> category.creator = resources.getString(R.string.ze) }
            expenseList.forEach { expense -> expense.creator = resources.getString(R.string.ze) }
            appDatabase.categoryDao().upsert(categoryList)
            appDatabase.expenseDao().upsert(expenseList)
            appDatabase.preferenceDao().upsert(
                Preference(
                    id = 1,
                    syncDateTime = Clock.System.now().toEpochMilliseconds()
                )
            )
        }

    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    fun syncAsServer() = viewModelScope.launch(Dispatchers.IO) {
        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                _messageOfServer.value =
                    _messageOfServer.value.plus(resources.getString(R.string.msg_service_registered))
                        .plus(serviceInfo.serviceName).plus('\n')
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        val serverSocket = ServerSocket(0)
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "SyncExpense"
            serviceType = "_csvr._tcp"
            port = serverSocket.localPort
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        val realServerSocket = serverSocket.accept()
        val objectOutputStream = ObjectOutputStream(realServerSocket.outputStream)
        val objectInputStream = ObjectInputStream(realServerSocket.inputStream)
        val sendDb = SyncInfo(
            message = "DB",
            categoryList = appDatabase.categoryDao()
                .getAllOfMyNew(resources.getString(R.string.me)),
            expenseList = appDatabase.expenseDao().getAllOfMyNew(resources.getString(R.string.me)),
        )
        objectOutputStream.writeObject(sendDb)
        val receivedSyncInfo = objectInputStream.readObject() as SyncInfo
        if (receivedSyncInfo.message == "DB") {
            saveNewDb(receivedSyncInfo.categoryList, receivedSyncInfo.expenseList)
            val sendDone = SyncInfo(
                message = "DONE",
                categoryList = emptyList(),
                expenseList = emptyList()
            )
            objectOutputStream.writeObject(sendDone)
            _messageOfServer.value =
                _messageOfServer.value.plus(resources.getString(R.string.done))
            nsdManager.unregisterService(registrationListener)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 17)
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            _messageOfClient.value =
                _messageOfClient.value.plus(resources.getString(R.string.msg_service_resolved))
                    .plus(serviceInfo).plus('\n')
            if (serviceInfo.serviceName.contains("SyncExpense") && serviceInfo.hostAddresses.isNotEmpty()) {
                val ipAddress = serviceInfo.hostAddresses[0].hostAddress
                val port = serviceInfo.port
                val clientSocket = Socket(ipAddress, port)
                val objectOutputStream = ObjectOutputStream(clientSocket.outputStream)
                val objectInputStream = ObjectInputStream(clientSocket.inputStream)
                var receivedSyncInfo = objectInputStream.readObject() as SyncInfo
                if (receivedSyncInfo.message == "DB") {
                    saveNewDb(receivedSyncInfo.categoryList, receivedSyncInfo.expenseList)
                    val sendDb = SyncInfo(
                        message = "DB",
                        categoryList = appDatabase.categoryDao()
                            .getAllOfMyNew(resources.getString(R.string.me)),
                        expenseList = appDatabase.expenseDao()
                            .getAllOfMyNew(resources.getString(R.string.me)),
                    )
                    objectOutputStream.writeObject(sendDb)
                    receivedSyncInfo = objectInputStream.readObject() as SyncInfo
                    if (receivedSyncInfo.message == "DONE") {
                        _messageOfClient.value =
                            _messageOfClient.value.plus(resources.getString(R.string.done))
                        cleanup()
                    }
                }
            }
        }
    }
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            _messageOfClient.value =
                _messageOfClient.value.plus(resources.getString(R.string.msg_discovery_started))
                    .plus('\n')
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            _messageOfClient.value =
                _messageOfClient.value.plus(resources.getString(R.string.msg_discovery_succeed))
                    .plus(serviceInfo.serviceName).plus('\n')
            if (serviceInfo.serviceName.contains("SyncExpense")) {
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun syncAsClient() {
        nsdManager.discoverServices("_csvr._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    fun cleanup() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
}

data class SyncInfo(
    val message: String,
    val categoryList: List<Category>,
    val expenseList: List<Expense>,
) : Serializable