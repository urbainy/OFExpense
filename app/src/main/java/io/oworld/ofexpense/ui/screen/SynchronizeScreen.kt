package io.oworld.ofexpense.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.time.Clock
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun SynchronizeScreen(
    paddingValues: PaddingValues, viewModel: SynchronizeViewModel = hiltViewModel(
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

    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        Button(onClick = { viewModel.syncAsServer() }, Modifier.fillMaxWidth()) {
            Text(getStr(R.string.sync_as_server), fontSize = 13.sp)
        }
        Text(
            messageOfServer,
            fontSize = 12.sp,
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .verticalScroll(serverMessageScroll)
        )
        Spacer(Modifier.width(36.dp))
        Button(onClick = { viewModel.syncAsClient() }, Modifier.fillMaxWidth()) {
            Text(getStr(R.string.sync_as_client), fontSize = 13.sp)
        }
        Text(
            messageOfClient,
            fontSize = 12.sp,
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()
                .verticalScroll(clientMessageScroll)
        )
    }
}

@HiltViewModel
class SynchronizeViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val _messageOfServer = MutableStateFlow("")
    val messageOfServer = _messageOfServer.asStateFlow()
    private val _messageOfClient = MutableStateFlow("")
    val messageOfClient = _messageOfClient.asStateFlow()
    private val resources = appContext.resources

    @OptIn(ExperimentalTime::class)
    private fun saveZeDb(categoryList: List<Category>, expenseList: List<Expense>) =
        viewModelScope.launch(Dispatchers.IO) {
            categoryList.forEach { category ->
                val temp = category.myShare
                category.myShare = category.zeShare
                category.zeShare = temp
                if (category.creator == resources.getString(R.string.ze)) {
                    category.creator = resources.getString(R.string.me)
                } else if (category.creator == resources.getString(R.string.me)) {
                    category.creator = resources.getString(R.string.ze)
                }
            }
            expenseList.forEach { expense ->
                if (expense.creator == resources.getString(R.string.ze)) {
                    expense.creator = resources.getString(R.string.me)
                } else if (expense.creator == resources.getString(R.string.me)) {
                    expense.creator = resources.getString(R.string.ze)
                }
            }
            appDatabase.categoryDao().upsert(categoryList)
            appDatabase.expenseDao().upsert(expenseList)
        }

    private fun updateSyncTime() = viewModelScope.launch(Dispatchers.IO) {
        val preference = appDatabase.preferenceDao().get()
        appDatabase.preferenceDao()
            .upsert(preference!!.copy(syncDateTime = Clock.systemUTC().millis()))
    }

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


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

        //1.Server sends sync timestamp expecting new records.
        val lastSyncMillis = SyncInfo(
            message = "LAST_SYNC_MILLIS",
            lastSyncMillis = appDatabase.preferenceDao().get()!!.syncDateTime,
            categoryList = emptyList(),
            expenseList = emptyList(),
        )
        objectOutputStream.writeObject(lastSyncMillis)

        //3. Server receives client's db, save it.
        var receivedSyncInfo = objectInputStream.readObject() as SyncInfo
        saveZeDb(receivedSyncInfo.categoryList, receivedSyncInfo.expenseList)

        //4. Server sends db.
        val sendDb = SyncInfo(
            message = "DB",
            lastSyncMillis = 0,
            categoryList = appDatabase.categoryDao()
                .getAllNew(receivedSyncInfo.lastSyncMillis),
            expenseList = appDatabase.expenseDao()
                .getAllNew(receivedSyncInfo.lastSyncMillis),
        )
        objectOutputStream.writeObject(sendDb)

        //7. Server receives done, update sync timestamp.
        receivedSyncInfo = objectInputStream.readObject() as SyncInfo
        if (receivedSyncInfo.message == "DONE") {
            updateSyncTime()
            _messageOfServer.value =
                _messageOfServer.value.plus(resources.getString(R.string.done)).plus('\n')
            nsdManager.unregisterService(registrationListener)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 17)
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val linkProperties =
                connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
            val myIpAddress =
                linkProperties?.linkAddresses?.find { linkAddress -> linkAddress.address is Inet4Address }?.address
            if (serviceInfo.serviceName.contains("SyncExpense")) {
                val ipAddress = serviceInfo.host
                val port = serviceInfo.port
                if (ipAddress == myIpAddress) {
                    _messageOfClient.value =
                        _messageOfClient.value.plus(resources.getString(R.string.msg_self_service))
                } else {
                    _messageOfClient.value =
                        _messageOfClient.value.plus(resources.getString(R.string.msg_service_resolved))
                            .plus(serviceInfo.serviceName).plus('\n').plus(ipAddress).plus('\n')
                            .plus(port.toString()).plus('\n')
                    val clientSocket = Socket(ipAddress, port)
                    val objectOutputStream = ObjectOutputStream(clientSocket.outputStream)
                    val objectInputStream = ObjectInputStream(clientSocket.inputStream)

                    //2. Client sends all new records after server's last sync timestamp, including both sides.
                    var receivedSyncInfo = objectInputStream.readObject() as SyncInfo
                    if (receivedSyncInfo.message == "LAST_SYNC_MILLIS") {
                        val zeSyncMillis = receivedSyncInfo.lastSyncMillis
                        val db = SyncInfo(
                            message = "DB",
                            lastSyncMillis = appDatabase.preferenceDao().get()!!.syncDateTime,
                            categoryList = appDatabase.categoryDao().getAllNew(zeSyncMillis),
                            expenseList = appDatabase.expenseDao().getAllNew(zeSyncMillis),
                        )
                        objectOutputStream.writeObject(db)
                    }

                    //5. Client receives server's db, save it.
                    receivedSyncInfo = objectInputStream.readObject() as SyncInfo
                    if (receivedSyncInfo.message == "DB") {
                        saveZeDb(receivedSyncInfo.categoryList, receivedSyncInfo.expenseList)
                    }

                    //6. Client sends DONE to server and update sync timestamp.
                    val done = SyncInfo(
                        message = "DONE",
                        lastSyncMillis = 0,
                        categoryList = emptyList(),
                        expenseList = emptyList(),
                    )
                    objectOutputStream.writeObject(done)
                    updateSyncTime()
                    _messageOfClient.value =
                        _messageOfClient.value.plus(resources.getString(R.string.done)).plus('\n')
                    stopDiscoveryListener()
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
    fun stopDiscoveryListener() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
}

data class SyncInfo(
    val message: String,
    val lastSyncMillis: Long,
    val categoryList: List<Category>,
    val expenseList: List<Expense>,
) : Serializable