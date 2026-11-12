package sa.factory.iiotindustrialiot1.data.repository

import sa.factory.iiotindustrialiot1.domain.repository.IiotIndustrialRepository
import sa.factory.iiotindustrialiot1.hardware.HardwareSensorDriver
import sa.factory.iiotindustrialiot1.model.IiotIndustrialItem
import sa.factory.iiotindustrialiot1.model.IiotIndustrialOrder
import sa.factory.iiotindustrialiot1.model.IiotIndustrialTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IiotIndustrialRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : IiotIndustrialRepository {

    private val _catalog = MutableStateFlow<List<IiotIndustrialItem>>(listOf(
        IiotIndustrialItem("1", "تحليل طيفي ثلاثي المحاور", "تحليل ترددي متعمق لبيانات الاهتزاز عبر مستشعرات تسارع صناعية", 150.0, "معتمد", "تحليل الاهتزازات"),
        IiotIndustrialItem("2", "رصد ضوضائي صناعي", "كشف فوري للانحرافات الصوتية عبر مسبار Modbus RS485", 280.0, "فوري", "مراقبة الصوت"),
        IiotIndustrialItem("3", "توقعات الصيانة التنبؤية", "خوارزميات ذكاء اصطناعي لتحديد أعطال المعدات قبل حدوثها", 95.0, "جديد", "تقارير الصيانة")
    ))
    override val catalogFlow: Flow<List<IiotIndustrialItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<IiotIndustrialItem>>(emptyList())
    override val activeCart: StateFlow<List<IiotIndustrialItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<IiotIndustrialTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            IiotIndustrialTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: IiotIndustrialItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: IiotIndustrialItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): IiotIndustrialOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = IiotIndustrialOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
