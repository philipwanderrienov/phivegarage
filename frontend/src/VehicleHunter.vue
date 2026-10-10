<script setup>
import {ref,computed,onMounted,watch} from 'vue'
const props=defineProps({api:{type:Function,required:true}})
const emit=defineEmits(['decision'])
const keywordOptions=['BU','Butuh Uang','Butuh dana cepat','Jual cepat','Jual rugi','Pemakaian pribadi','Atas nama pribadi','Milik pribadi','Lanjut rawat','Lanjut ngerawatin','Nerusin','Terusin']
const enabledKeywords=ref([...keywordOptions])
const keywordOnly=ref(true)
const keywordQuery=ref('')
const matchingListings=computed(()=>{
 const q=keywordQuery.value.trim().toLowerCase()
 return list.value.filter(x=>{
  if(q && ![x.title,x.brand,x.model,x.notes,x.location].some(v=>String(v||'').toLowerCase().includes(q)))return false
  if(!keywordOnly.value)return true
  return (x.matchedKeywords||[]).some(k=>enabledKeywords.value.includes(k))
 }).sort((a,b)=>(b.keywordPriority||0)-(a.keywordPriority||0))
})
function searchPhrase(keyword){
 const query=[keyword,keywordQuery.value,'mobil bekas'].filter(Boolean).join(' ')
 window.open('https://www.google.com/search?q='+encodeURIComponent(query),'_blank','noopener,noreferrer')
}
const form=ref({source:'FACEBOOK_MANUAL',sourceUrl:'',title:'',brand:'Toyota',model:'',year:2005,askingPrice:45000000,kilometer:null,location:'Jabodetabek',transmission:'MANUAL',stnk:'UNKNOWN',bpkb:'UNKNOWN',notes:''})
const budget=ref(null),targetProfit=ref(null),repairCost=ref(null),taxCost=ref(null),transportCost=ref(null),riskBuffer=ref(null),buyer=ref('RETAIL'),list=ref([]),selected=ref(null),evaluation=ref(null),compPrice=ref(null),compUrl=ref(''),error=ref(''),busy=ref(false)
watch([budget,targetProfit,repairCost,taxCost,transportCost,riskBuffer,buyer],()=>{evaluation.value=null})
const idr=n=>n==null?'Belum ada data':new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(n)
async function work(fn){busy.value=true;error.value='';try{await fn()}catch(e){error.value=e.message||'Request gagal'}finally{busy.value=false}}
async function load(){list.value=await props.api('/hunter/listings')}
async function add(){await work(async()=>{const x=await props.api('/hunter/listings',{method:'POST',body:JSON.stringify(form.value)});await load();selected.value=x.id;evaluation.value=null})}
async function choose(row){selected.value=row.id;evaluation.value=null;if(budget.value!==null&&targetProfit.value!==null)await evaluate()}
function evaluationUrl(){
 if(budget.value===null||budget.value===''||!Number.isSafeInteger(Number(budget.value))||Number(budget.value)<=0)throw Error('Masukkan budget positif dalam rupiah.')
 if(targetProfit.value===null||targetProfit.value===''||!Number.isSafeInteger(Number(targetProfit.value))||Number(targetProfit.value)<0)throw Error('Masukkan target laba dalam rupiah (minimum 0).')
 const costs={repairCost,taxCost,transportCost,riskBuffer}
 for(const [key,field] of Object.entries(costs)){
  if(field.value===null||field.value===''||!Number.isSafeInteger(Number(field.value))||Number(field.value)<0)
   throw Error('Isi '+key+' dengan nominal rupiah minimum 0.')
 }
 const params=new URLSearchParams({budget:String(budget.value),targetProfit:String(targetProfit.value),
 repairCost:String(repairCost.value),taxCost:String(taxCost.value),transportCost:String(transportCost.value),riskBuffer:String(riskBuffer.value),buyer:buyer.value})
 return '/hunter/listings/'+selected.value+'/evaluate?'+params.toString()
}
async function evaluate(){if(!selected.value)return;await work(async()=>{const url=evaluationUrl();evaluation.value=await props.api(url)})}
function sendToDecision(){
 if(!evaluation.value)return
 const l=evaluation.value.listing
 emit('decision',{vehicle:[l.brand,l.model].filter(Boolean).join(' '),year:l.manufacture_year??'',
 kilometer:l.kilometer??'',transmission:l.transmission||'UNKNOWN',stnk:l.stnk_status,bpkb:l.bpkb_status,
 askPrice:l.asking_price,capital:Number(budget.value),maxPurchase:evaluation.value.maxBuyPrice??Number(budget.value),
 targetProfit:Number(targetProfit.value),repairLow:Number(repairCost.value),repairHigh:Number(repairCost.value),
 taxCost:Number(taxCost.value),otherCosts:Number(transportCost.value),riskBuffer:Number(riskBuffer.value),
 saleLow:evaluation.value.quickSaleEstimate??'',saleMid:evaluation.value.medianAskingPrice??'',
 saleHigh:evaluation.value.medianAskingPrice??'',marketEvidence:l.source_url||'',
 evidenceConfirmed:false,inspectionConfirmed:false,purchaseType:'DIRECT',
 notes:[l.notes||'',l.source_url||''].filter(Boolean).join('\\n')})
}
async function setStatus(status){
 if(!selected.value)return
 await work(async()=>{
  await props.api('/hunter/listings/'+selected.value+'/status',{method:'PATCH',body:JSON.stringify({status})})
  await load()
  evaluation.value=null
 })
}
async function addComparable(){if(!selected.value)return;await work(async()=>{await props.api('/hunter/listings/'+selected.value+'/comparables',{method:'POST',body:JSON.stringify({price:Number(compPrice.value),sourceUrl:compUrl.value||null})});compPrice.value=null;compUrl.value='';if(budget.value!==null&&targetProfit.value!==null)evaluation.value=await props.api(evaluationUrl())})}
onMounted(()=>work(load))
</script>
<template>
<section class="hunter">
 <div class="page-heading"><div><span class="eyebrow">MULTI-SOURCE INTELLIGENCE</span><h1>Vehicle Hunter</h1><p>Catat listing asli, kumpulkan pembanding, dan hitung batas beli yang aman. Tidak melakukan scraping otomatis.</p></div></div>
 <div v-if="error" class="error banner">{{error}}</div>
 <div class="hunter-grid">
  <section class="panel">
   <h3>Tambah kandidat</h3>
   <form class="hunter-form" @submit.prevent="add">
    <label>Sumber<select v-model="form.source"><option value="FACEBOOK_MANUAL">Facebook Marketplace (input manual)</option><option value="OLX_MANUAL">OLX (input manual)</option><option value="AUCTION_MANUAL">Lelang (input manual)</option><option value="OTHER_MANUAL">Lainnya</option></select></label>
    <label>Link iklan asli<input type="url" v-model="form.sourceUrl" placeholder="https://..."></label>
    <label>Judul listing<input required v-model="form.title"></label>
    <label>Merek<input required v-model="form.brand"></label>
    <label>Model<input required v-model="form.model"></label>
    <label>Tahun<input type="number" min="1950" max="2100" v-model.number="form.year"></label>
    <label>Harga penawaran (Rp)<input type="number" min="1" required v-model.number="form.askingPrice"></label>
    <label>Lokasi<input v-model="form.location"></label>
    <label>STNK<select v-model="form.stnk"><option>UNKNOWN</option><option>ADA</option><option>TIDAK_ADA</option></select></label>
    <label>BPKB<select v-model="form.bpkb"><option>UNKNOWN</option><option>ADA</option><option>TIDAK_ADA</option></select></label>
    <button class="primary" type="submit" :disabled="busy">Simpan kandidat</button>
   </form>
  </section>
  <section class="panel">
   <h3>Kandidat tersimpan ({{matchingListings.length}} dari {{list.length}})</h3>
   <div class="hunter-keywords"><b>Fokus keyword hunting</b><p class="muted">Pilih kata yang diprioritaskan. Pencarian web dibuka manual, tidak melakukan scraping.</p>
    <label v-for="keyword in keywordOptions" :key="keyword" class="hunter-chip"><input type="checkbox" :value="keyword" v-model="enabledKeywords">{{keyword}} <button type="button" @click.stop="searchPhrase(keyword)" title="Cari di web">↗</button></label>
    <label><input type="checkbox" v-model="keywordOnly"> Hanya kandidat dengan keyword terpilih</label>
    <input v-model="keywordQuery" placeholder="Filter model, daerah, atau teks listing">
   </div>
   <p v-if="!list.length">Belum ada listing. Tambahkan kandidat dari sumber yang dapat diperiksa.</p>
   <button class="hunter-result" v-for="item in matchingListings" :key="item.id" @click="choose(item)">
    <b>{{item.brand}} {{item.model}} {{item.manufacture_year}}</b><span>{{idr(item.asking_price)}}</span><small>{{item.source}} · {{item.location}}</small><small v-if="item.matchedKeywords?.length">Keyword: {{item.matchedKeywords.join(", ")}}</small>
   </button>
  </section>
 </div>
 <section class="panel" v-if="selected">
 <div class="hunter-controls">
  <label>Status iklan (verifikasi manual)
   <select :value="list.find(x=>x.id===selected)?.listing_status||'UNVERIFIED'" @change="setStatus($event.target.value)">
    <option value="UNVERIFIED">Belum diverifikasi</option><option value="ACTIVE">Aktif — dikonfirmasi</option>
    <option value="SOLD">Terjual</option><option value="REMOVED">Dihapus</option>
   </select>
  </label>
  <p>Status aktif adalah pernyataan user, bukan pengecekan otomatis Facebook.</p>
 </div>
  <h3>Analisis finansial</h3>
  <div class="hunter-controls"><label>Modal maksimal (Rp)<input type="number" min="1" step="1" required placeholder="Masukkan modal" v-model.number="budget"></label><label>Target profit bersih (Rp)<input type="number" min="0" step="1" required placeholder="Masukkan target laba" v-model.number="targetProfit"></label><label>Estimasi servis (Rp)<input type="number" min="0" step="1" v-model.number="repairCost" placeholder="Isi biaya"></label><label>Pajak dan dokumen (Rp)<input type="number" min="0" step="1" v-model.number="taxCost" placeholder="Isi biaya"></label><label>Transport & iklan (Rp)<input type="number" min="0" step="1" v-model.number="transportCost" placeholder="Isi biaya"></label><label>Cadangan risiko (Rp)<input type="number" min="0" step="1" v-model.number="riskBuffer" placeholder="Isi biaya"></label><label>Target pembeli<select v-model="buyer"><option value="RETAIL">Retail</option><option value="DEALER">Pedagang</option></select></label><button class="secondary" @click="evaluate" :disabled="busy">Hitung ulang</button></div>
  <div v-if="evaluation">
   <h3>Keputusan: {{evaluation.decision}}</h3><button class="primary" type="button" @click="sendToDecision">Lanjut ke Keputusan Beli</button>
   <div class="hunter-stats"><div><small>Harga jual cepat (estimasi)</small><b>{{idr(evaluation.quickSaleEstimate)}}</b></div><div><small>Batas beli maksimal</small><b>{{idr(evaluation.maxBuyPrice)}}</b></div><div><small>Profit di harga iklan</small><b>{{idr(evaluation.estimatedProfitAtAsk)}}</b></div><div><small>Jumlah pembanding</small><b>{{evaluation.comparableCount}}</b></div></div>
   <p v-for="reason in evaluation.reasons" :key="reason">• {{reason}}</p>
   <a v-if="evaluation.listing.source_url" :href="evaluation.listing.source_url" target="_blank" rel="noopener noreferrer">Buka iklan asli ↗</a>
  </div>
  <form class="hunter-controls" @submit.prevent="addComparable"><label>Harga pembanding (Rp)<input type="number" min="1" required v-model.number="compPrice"></label><label>Link pembanding<input type="url" v-model="compUrl"></label><button class="primary" :disabled="busy">Tambahkan pembanding</button></form>
 </section>
</section>
</template>
<style scoped>
.hunter-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.hunter-form{display:grid;grid-template-columns:1fr 1fr;gap:12px}.hunter-form label,.hunter-controls label{display:flex;flex-direction:column;gap:5px;font-size:13px}.hunter-form input,.hunter-form select,.hunter-controls input{width:100%;padding:10px;min-width:0}.hunter-controls{display:flex;gap:14px;flex-wrap:wrap;align-items:end;margin:18px 0}.hunter-controls label{flex:1;min-width:180px}.hunter-result{display:flex;flex-direction:column;text-align:left;width:100%;padding:12px;margin:9px 0;border:1px solid #7775;border-radius:8px;background:transparent;color:inherit;cursor:pointer}.hunter-result span{font-weight:bold}.hunter-result small{opacity:.7}.hunter-stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:18px 0}.hunter-stats>div{display:flex;flex-direction:column;gap:8px;padding:12px;border:1px solid #7775;border-radius:8px}.hunter-stats b{font-size:18px}.hunter-keywords{display:flex;flex-wrap:wrap;gap:8px;margin:12px 0}.hunter-keywords>b,.hunter-keywords>p,.hunter-keywords>input{width:100%}.hunter-chip{border:1px solid #7776;border-radius:8px;padding:7px;display:flex;align-items:center;gap:6px;font-size:12px}.hunter-chip button{border:0;background:transparent;color:inherit;cursor:pointer}.hunter-keywords>label:not(.hunter-chip){width:100%;font-size:13px}@media(max-width:900px){.hunter-grid,.hunter-stats{grid-template-columns:1fr}.hunter-form{grid-template-columns:1fr}}
</style>