package pl.smartpilot.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
 private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
 private var socket: WebSocket? = null
 private lateinit var status: TextView
 private lateinit var address: EditText
 private var connected = false
 private val prefs by lazy { getSharedPreferences("devices", MODE_PRIVATE) }
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 28, 22, 20); setBackgroundColor(Color.rgb(16,20,29)) }
  val scroll = ScrollView(this); scroll.addView(root); setContentView(scroll)
  fun label(t:String, size:Float=17f): TextView = TextView(this).apply { text=t; textSize=size; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(8,14,8,14) }
  root.addView(label("SMART PILOT PRO", 26f)); root.addView(label("Samsung UE50RU7102KXXH", 16f))
  address = EditText(this).apply { hint="Adres IP telewizora, np. 192.168.1.100"; setSingleLine(); setText(prefs.getString("tv_ip", "")); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY) }; root.addView(address)
  status=label("Wpisz IP telewizora i połącz", 14f); root.addView(status)
  fun button(title:String, action:()->Unit): Button = Button(this).apply { text=title; textSize=17f; setOnClickListener { action() } }
  root.addView(button("POŁĄCZ Z TELEWIZOREM") { connect() })
  root.addView(label("Pilot TV", 21f))
  fun row(vararg entries:Pair<String,String>) { val line=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }; entries.forEach { (name,key)-> val b=button(name){sendKey(key)}; line.addView(b,LinearLayout.LayoutParams(0,60,1f)) }; root.addView(line) }
  row("⏻" to "KEY_POWER", "HOME" to "KEY_HOME", "ŹRÓDŁO" to "KEY_SOURCE")
  row("VOL −" to "KEY_VOLDOWN", "WYCISZ" to "KEY_MUTE", "VOL +" to "KEY_VOLUP")
  row("CH −" to "KEY_CHDOWN", "MENU" to "KEY_MENU", "CH +" to "KEY_CHUP")
  row(" " to "", "▲" to "KEY_UP", " " to "")
  row("◀" to "KEY_LEFT", "OK" to "KEY_ENTER", "▶" to "KEY_RIGHT")
  row("WSTECZ" to "KEY_RETURN", "▼" to "KEY_DOWN", "EXIT" to "KEY_EXIT")
  root.addView(label("Soundbar HW-Q800T i PLAY BOX DV8990-T2/C: obsługa wymaga potwierdzenia protokołu. Te przyciski nie są jeszcze aktywne.", 14f))
  root.addView(button("SOUNDBAR — W PRZYGOTOWANIU") {}.apply { isEnabled=false })
  root.addView(button("PLAY BOX — W PRZYGOTOWANIU") {}.apply { isEnabled=false })
  val saved=prefs.getString("tv_ip", "") ?: ""; if(saved.isNotBlank()) connect()
 }
 private fun update(message:String) { runOnUiThread { status.text=message } }
 private fun connect() {
  val ip=address.text.toString().trim(); if(!Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$").matches(ip) || ip.split('.').any { it.toInt()>255 }) { update("Podaj poprawny adres IPv4"); return }
  prefs.edit().putString("tv_ip",ip).apply(); socket?.close(1000,"Reconnect"); connected=false
  val url="wss://$ip:8002/api/v2/channels/samsung.remote.control?name="+android.util.Base64.encodeToString("Smart Pilot PRO".toByteArray(),android.util.Base64.NO_WRAP)
  update("Łączenie... zaakceptuj pilota na ekranie TV")
  socket=client.newWebSocket(Request.Builder().url(url).build(),object:WebSocketListener() {
   override fun onOpen(webSocket:WebSocket,response:Response) { connected=true; update("Połączono z TV — jeśli pojawi się prośba, zatwierdź na telewizorze") }
   override fun onMessage(webSocket:WebSocket,text:String) { try { val obj=JSONObject(text); if(obj.optString("event")=="ms.channel.connect") update("Telewizor połączony ✓") } catch(_:Exception){} }
   override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?) { connected=false; update("Nie udało się połączyć: sprawdź IP, Wi-Fi i zgodę na TV") }
   override fun onClosed(webSocket:WebSocket,code:Int,reason:String) { connected=false; update("Rozłączono") }
  })
 }
 private fun sendKey(key:String) { if(key.isEmpty())return; if(!connected){update("Najpierw połącz telewizor");return}; val payload=JSONObject().put("method","ms.remote.control").put("params",JSONObject().put("Cmd","Click").put("DataOfCmd",key).put("Option","false").put("TypeOfRemote","SendRemoteKey")); socket?.send(payload.toString()) }
 override fun onDestroy(){socket?.close(1000,"Exit");client.dispatcher.executorService.shutdown();super.onDestroy()}
}