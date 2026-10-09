<script setup>
import {ref,onMounted} from 'vue'
const props=defineProps({api:{type:Function,required:true}})
const form=ref({source:'FACEBOOK_MANUAL',sourceUrl:'',title:'',brand:'Toyota',model:'',year:2005,askingPrice:45000000,kilometer:null,location:'Jabodetabek',transmission:'MANUAL',stnk:'UNKNOWN',bpkb:'UNKNOWN',notes:''})
const budget=ref(50000000),targetProfit=ref(8000000),list=ref([]),selected=ref(null),evaluation=ref(null),compPrice=ref(null),compUrl=ref(''),error=ref(''),busy=ref(false)
const idr=n=>n==null?'Belum ada data':new Intl.NumberFormat('id-ID',{style:'currency',currency:'IDR',maximumFractionDigits:0}).format(n)
async function work(fn){busy.value=true;error.value='';try{await fn()}catch(e){error.value=e.message||'Request gagal'}finally{busy.value=false}}
async function load(){list.value=await props.api('/hunter/listings')}
async function add(){await work(async()=>{const x=await props.api('/hunter/listings',{method:'POST',body:JSON.stringify(form.value)});await load();selected.value=x.id;await evaluate()})}
async function choose(row){selected.value=row.id;await evaluate()}
async function evaluate(){if(!selected.value)return;await work(async()=>{evaluation.value=await props.api('/hunter/listings/'+selected.value+'/evaluate?budget='+budget.value+'&targetProfit='+targetProfit.value)})}
async function addComparable(){if(!selected.value)return;await work(async()=>{await props.api('/hunter/listings/'+selected.value+'/comparables',{method:'POST',body:JSON.stringify({price:Number(compPrice.value),sourceUrl:compUrl.value||null})});compPrice.value=null;compUrl.value='';evaluation.value=await props.api('/hunter/listings/'+selected.value+'/evaluate?budget='+budget.value+'&targetProfit='+targetProfit.value)})}
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
   <h3>Kandidat tersimpan ({{list.length}})</h3>
   <p v-if="!list.length">Belum ada listing. Tambahkan kandidat dari sumber yang dapat diperiksa.</p>
   <button class="hunter-result" v-for="item in list" :key="item.id" @click="choose(item)">
    <b>{{item.brand}} {{item.model}} {{item.manufacture_year}}</b><span>{{idr(item.asking_price)}}</span><small>{{item.source}} · {{item.location}}</small>
   </button>
  </section>
 </div>
 <section class="panel" v-if="selected">
  <h3>Analisis finansial</h3>
  <div class="hunter-controls"><label>Modal maksimal (Rp)<input type="number" min="0" v-model.number="budget"></label><label>Target profit bersih (Rp)<input type="number" min="0" v-model.number="targetProfit"></label><button class="secondary" @click="evaluate" :disabled="busy">Hitung ulang</button></div>
  <div v-if="evaluation">
   <h3>Keputusan: {{evaluation.decision}}</h3>
   <div class="hunter-stats"><div><small>Harga jual cepat (estimasi)</small><b>{{idr(evaluation.quickSaleEstimate)}}</b></div><div><small>Batas beli maksimal</small><b>{{idr(evaluation.maxBuyPrice)}}</b></div><div><small>Profit di harga iklan</small><b>{{idr(evaluation.estimatedProfitAtAsk)}}</b></div><div><small>Jumlah pembanding</small><b>{{evaluation.comparableCount}}</b></div></div>
   <p v-for="reason in evaluation.reasons" :key="reason">• {{reason}}</p>
   <a v-if="evaluation.listing.source_url" :href="evaluation.listing.source_url" target="_blank" rel="noopener noreferrer">Buka iklan asli ↗</a>
  </div>
  <form class="hunter-controls" @submit.prevent="addComparable"><label>Harga pembanding (Rp)<input type="number" min="1" required v-model.number="compPrice"></label><label>Link pembanding<input type="url" v-model="compUrl"></label><button class="primary" :disabled="busy">Tambahkan pembanding</button></form>
 </section>
</section>
</template>
<style scoped>
.hunter-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.hunter-form{display:grid;grid-template-columns:1fr 1fr;gap:12px}.hunter-form label,.hunter-controls label{display:flex;flex-direction:column;gap:5px;font-size:13px}.hunter-form input,.hunter-form select,.hunter-controls input{width:100%;padding:10px;min-width:0}.hunter-controls{display:flex;gap:14px;flex-wrap:wrap;align-items:end;margin:18px 0}.hunter-controls label{flex:1;min-width:180px}.hunter-result{display:flex;flex-direction:column;text-align:left;width:100%;padding:12px;margin:9px 0;border:1px solid #7775;border-radius:8px;background:transparent;color:inherit;cursor:pointer}.hunter-result span{font-weight:bold}.hunter-result small{opacity:.7}.hunter-stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:18px 0}.hunter-stats>div{display:flex;flex-direction:column;gap:8px;padding:12px;border:1px solid #7775;border-radius:8px}.hunter-stats b{font-size:18px}@media(max-width:900px){.hunter-grid,.hunter-stats{grid-template-columns:1fr}.hunter-form{grid-template-columns:1fr}}
</style>