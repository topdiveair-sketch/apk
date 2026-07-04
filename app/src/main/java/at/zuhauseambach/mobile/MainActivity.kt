package at.zuhauseambach.mobile

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Guest(val id:String,var name:String,var phone:String="")
data class Booking(val id:String,var guestId:String,var room:String,var arrival:String,var departure:String,var persons:Int,var price:Double,var breakfast:String)
data class Extra(val text:String,val price:Double,val qty:Int=1)

class MainActivity:AppCompatActivity(){
 private val rooms=listOf("DZ Bachblick","Marillenzimmer","Weinbergzimmer","Donauzimmer")
 private val drinks=listOf("Cola" to 3.5,"Fanta" to 3.5,"Bier" to 4.0,"Radler" to 4.0,"Wein 1/8" to 3.8,"Mineral" to 2.8,"Kaffee" to 2.8)
 private val foods=listOf("Frühstück" to 12.0,"Gulasch" to 12.0,"Schinkenplatte" to 11.0,"Salat" to 7.5,"Grillteller klein" to 14.0,"Marillenkuchen" to 4.5)
 private val guests=mutableListOf<Guest>()
 private val bookings=mutableListOf<Booking>()
 private val extras=mutableMapOf<String,MutableList<Extra>>()
 private var activeBookingId:String?=null
 private lateinit var root:LinearLayout
 override fun onCreate(b:Bundle?){super.onCreate(b);loadData();showHome()}
 private fun screen(title:String){
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(246,242,234))}
  root.addView(TextView(this).apply{text="🏡 Zuhause am Bach Mobile\n$title";textSize=22f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(24,26,24,22);setBackgroundColor(Color.rgb(47,93,80))})
  val scroll=ScrollView(this); val cont=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,18,18,18);tag="content"}; scroll.addView(cont); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.WHITE)}
  listOf("Heute" to {showHome()},"Buchung" to {showBookings()},"Extras" to {showExtras()},"Rechnung" to {showInvoice()}).forEach{(t,f)->nav.addView(Button(this).apply{text=t;textSize=14f;setOnClickListener{f()}},LinearLayout.LayoutParams(0,78,1f))}
  root.addView(nav);setContentView(root)
 }
 private fun cont()=((root.getChildAt(1)as ScrollView).getChildAt(0)as LinearLayout)
 private fun btn(t:String,c:Int=Color.rgb(255,250,241),f:()->Unit)=Button(this).apply{text=t;textSize=21f;setTextColor(Color.rgb(31,43,37));setBackgroundColor(c);setPadding(12,18,12,18);setOnClickListener{f()}}
 private fun title(t:String)=TextView(this).apply{text=t;textSize=22f;setPadding(0,20,0,10)}
 private fun row(t:String,s:String="",f:(()->Unit)?=null){cont().addView(TextView(this).apply{text=if(s=="")t else "$t\n$s";textSize=18f;setPadding(18,16,18,16);setBackgroundColor(Color.rgb(255,250,241));if(f!=null)setOnClickListener{f()}},LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,10)})}
 fun showHome(){screen("Heute");val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};r.addView(btn("🛏 Buchungen",Color.rgb(223,241,233)){showBookings()},LinearLayout.LayoutParams(0,120,1f));r.addView(btn("👥 Gäste",Color.rgb(226,237,248)){showGuests()},LinearLayout.LayoutParams(0,120,1f));cont().addView(r);val r2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};r2.addView(btn("🍺 Extras",Color.rgb(255,240,206)){showExtras()},LinearLayout.LayoutParams(0,120,1f));r2.addView(btn("💶 Rechnung",Color.rgb(223,241,233)){showInvoice()},LinearLayout.LayoutParams(0,120,1f));cont().addView(r2);cont().addView(title("Zimmerstatus"));val today=today();rooms.forEach{rm->val b=bookings.find{it.room==rm&&it.arrival<=today&&it.departure>=today};row("🏡 $rm",if(b==null)"frei" else gname(b.guestId))}}
 fun showGuests(){screen("Gäste");cont().addView(btn("➕ Neuen Gast hinzufügen",Color.rgb(223,241,233)){guestDialog(null)});guests.sortedBy{it.name}.forEach{g->row("👤 ${g.name}",g.phone){guestDialog(g)}}}
 private fun guestDialog(g:Guest?){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,0)};val name=EditText(this).apply{hint="Name";setText(g?.name?:"")};val phone=EditText(this).apply{hint="Telefon";setText(g?.phone?:"")};box.addView(name);box.addView(phone);android.app.AlertDialog.Builder(this).setTitle(if(g==null)"Neuer Gast" else "Gast bearbeiten").setView(box).setPositiveButton("Speichern"){_,_->if(g==null)guests.add(Guest(id(),name.text.toString(),phone.text.toString()))else{g.name=name.text.toString();g.phone=phone.text.toString()};saveData();showGuests()}.setNegativeButton("Abbrechen",null).show()}
 fun showBookings(){screen("Buchungen");cont().addView(btn("➕ Neue Buchung",Color.rgb(223,241,233)){bookingDialog(null)});bookings.sortedBy{it.arrival}.forEach{b->row("🛏 ${gname(b.guestId)}","${b.room} · ${b.arrival} bis ${b.departure} · ${b.persons} Pers."){bookingDialog(b)}}}
 private fun bookingDialog(b:Booking?){if(guests.isEmpty())guests.add(Guest(id(),"Neuer Gast"));val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,0)};val gs=Spinner(this);gs.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,guests.map{it.name});val rs=Spinner(this);rs.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,rooms);val a=EditText(this).apply{hint="Anreise yyyy-mm-dd";setText(b?.arrival?:today())};val d=EditText(this).apply{hint="Abreise yyyy-mm-dd";setText(b?.departure?:tomorrow())};val pers=EditText(this).apply{hint="Personen";setText((b?.persons?:2).toString())};val price=EditText(this).apply{hint="Preis/Nacht";setText((b?.price?:89.0).toString())};val fr=EditText(this).apply{hint="Frühstück";setText(b?.breakfast?:"kein")};if(b!=null){gs.setSelection(guests.indexOfFirst{it.id==b.guestId}.coerceAtLeast(0));rs.setSelection(rooms.indexOf(b.room).coerceAtLeast(0))};listOf(gs,rs,a,d,pers,price,fr).forEach{box.addView(it)};android.app.AlertDialog.Builder(this).setTitle(if(b==null)"Neue Buchung" else "Buchung bearbeiten").setView(box).setPositiveButton("Speichern"){_,_->val room=rs.selectedItem.toString();val bid=b?.id?:id();val conflict=bookings.find{it.id!=bid&&it.room==room&&overlap(a.text.toString(),d.text.toString(),it.arrival,it.departure)};if(conflict!=null){toast("Doppelbelegung: $room");return@setPositiveButton};if(b==null)bookings.add(Booking(bid,guests[gs.selectedItemPosition].id,room,a.text.toString(),d.text.toString(),pers.text.toString().toIntOrNull()?:1,price.text.toString().toDoubleOrNull()?:0.0,fr.text.toString()))else{b.guestId=guests[gs.selectedItemPosition].id;b.room=room;b.arrival=a.text.toString();b.departure=d.text.toString();b.persons=pers.text.toString().toIntOrNull()?:1;b.price=price.text.toString().toDoubleOrNull()?:0.0;b.breakfast=fr.text.toString()};activeBookingId=bid;saveData();showBookings()}.setNegativeButton("Abbrechen",null).show()}
 fun showExtras(){screen("Extras");cont().addView(title("Buchung auswählen"));bookings.forEach{b->row("🛏 ${gname(b.guestId)}",b.room){activeBookingId=b.id;showExtras()}};cont().addView(title("Getränke"));drinks.forEach{(t,p)->cont().addView(btn("➕ $t  ${"%.2f".format(p)} €",Color.rgb(226,237,248)){addExtra(t,p)})};cont().addView(title("Speisen"));foods.forEach{(t,p)->cont().addView(btn("➕ $t  ${"%.2f".format(p)} €",Color.rgb(255,240,206)){addExtra(t,p)})}}
 private fun addExtra(t:String,p:Double){val bid=activeBookingId?:bookings.firstOrNull()?.id;if(bid==null){toast("Keine Buchung vorhanden");return};activeBookingId=bid;extras.getOrPut(bid){ mutableListOf()}.add(Extra(t,p));saveData();toast("$t hinzugefügt")}
 fun showInvoice(){screen("Rechnung");bookings.forEach{b->row("🧾 ${gname(b.guestId)}",b.room){activeBookingId=b.id;showInvoice()}};val b=bookings.find{it.id==activeBookingId}?:bookings.firstOrNull();if(b==null){row("Keine Buchung vorhanden");return};val lines=extras[b.id]?: mutableListOf();val rt=nights(b.arrival,b.departure)*b.price;val et=lines.sumOf{it.price*it.qty};cont().addView(title("Rechnung: ${gname(b.guestId)}"));row("Zimmer ${b.room}","${"%.2f".format(rt)} €");lines.forEach{row("${it.qty}× ${it.text}","${"%.2f".format(it.price*it.qty)} €")};row("GESAMT","${"%.2f".format(rt+et)} €")}
 private fun gname(id:String)=guests.find{it.id==id}?.name?:"Unbekannt";private fun id()=System.currentTimeMillis().toString(36)+Random().nextInt(9999).toString();private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(Date());private fun tomorrow()=SimpleDateFormat("yyyy-MM-dd",Locale.GERMANY).format(Date(System.currentTimeMillis()+86400000));private fun overlap(a1:String,d1:String,a2:String,d2:String)=a1<d2&&a2<d1;private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 private fun loadData(){val f=File(filesDir,"zab_mobile_data.json");if(!f.exists()){guests.add(Guest("G1","Devich Armin"));guests.add(Guest("G2","Paul Artner"));saveData();return};try{val o=JSONObject(f.readText());val ga=o.optJSONArray("guests")?:JSONArray();for(i in 0 until ga.length()){val g=ga.getJSONObject(i);guests.add(Guest(g.getString("id"),g.optString("name"),g.optString("phone")))};val ba=o.optJSONArray("bookings")?:JSONArray();for(i in 0 until ba.length()){val b=ba.getJSONObject(i);bookings.add(Booking(b.getString("id"),b.getString("guestId"),b.getString("room"),b.getString("arrival"),b.getString("departure"),b.optInt("persons",1),b.optDouble("price",0.0),b.optString("breakfast","kein")))}}catch(_:Exception){}}
 private fun saveData(){val o=JSONObject();val ga=JSONArray();guests.forEach{ga.put(JSONObject().put("id",it.id).put("name",it.name).put("phone",it.phone))};val ba=JSONArray();bookings.forEach{ba.put(JSONObject().put("id",it.id).put("guestId",it.guestId).put("room",it.room).put("arrival",it.arrival).put("departure",it.departure).put("persons",it.persons).put("price",it.price).put("breakfast",it.breakfast))};o.put("guests",ga);o.put("bookings",ba);File(filesDir,"zab_mobile_data.json").writeText(o.toString(2))}
}
