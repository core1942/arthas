<script setup lang="ts">
import {onMounted, ref, computed} from "vue";
import {Terminal} from "xterm"
import {FitAddon} from 'xterm-addon-fit';
import {WebglAddon} from "xterm-addon-webgl"
import {MenuAlt2Icon} from "@heroicons/vue/outline"
import fullPic from "~/assert/fullsc.png"
import arthasLogo from "~/assert/arthas.png"

const {isTunnel = false} = defineProps<{
    isTunnel?: boolean
}>();

let ws: WebSocket | undefined;
let intervalReadKey = -1;
const DEFAULT_SCROLL_BACK = 1000;
const MAX_SCROLL_BACK = 9999999;
const MIN_SCROLL_BACK = 1;
const ARTHAS_PORT = isTunnel ? "7777" : "8563";
const ip = ref("");
const port = ref("");
const iframe = ref(true);
const fullSc = ref(true);
const agentID = ref("");
const outputHerf = computed(() => {
    console.log(agentID.value);
    return isTunnel ? `proxy/${agentID.value}/arthas-output/` : `/arthas-output/`;
});
// const isTunnel = import.meta.env.MODE === 'tunnel'
const fitAddon = new FitAddon();
const webglAddon = new WebglAddon();
let xterm = new Terminal({allowProposedApi: true});

const decoder = new TextDecoder('utf-8');
const FAIL_EVENT = new Int8Array([0x58, 0x69, 0x24, 0x79, 0x70, 0x59, 0x57, 0x39, 0x56, 0x78, 0x45, 0x36, 0x37, 0x3f, 0x3e, 0x2a]);
const CANCEL_EVENT = new Int8Array([0x78, 0x4e, 0x6f, 0x09, 0x0a, 0x5a, 0x4a, 0x6a, 0x5e, 0x6f, 0x78, 0x69, 0x3e, 0x5a, 0x58, 0x6b]);
const END_EVENT = new Int8Array([0x37, 0x37, 0x20, 0x30, 0x20, 0x31, 0x20, 0x33, 0x31, 0x36, 0x32, 0x00, 0x18, 0x6b, 0x64, 0x62]);

const START_DOWNLOAD_EVENT = new Int8Array([0x7a, 0x72, 0x65, 0x61, 0x64, 0x6c, 0x69, 0x6e, 0x65, 0x2e, 0x63, 0x00, 0x33, 0x31, 0x36, 0x32]);

const START_UPLOAD_EVENT = new Int8Array([0x5a, 0x62, 0x69, 0x37, 0x54, 0x4c, 0x1a, 0x6b, 0x4c, 0x2d, 0x3e, 0x00, 0x3f, 0x3a, 0x1b, 0x30]);
const CONFIRM_EVENT = new Int8Array([0x2a, 0x2a, 0x18, 0x42, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x32, 0x33, 0x62, 0x65]);

onMounted(() => {
    ip.value = getUrlParam("ip") ?? window.location.hostname;
    port.value = getUrlParam("port") ?? ARTHAS_PORT;
    if (isTunnel) agentID.value = getUrlParam("agentId") ?? "";
    let _iframe = getUrlParam("iframe");
    if (_iframe && _iframe.trim() !== "false") iframe.value = false;

    startConnect(true);
    window.addEventListener("resize", function () {
        if (ws !== undefined && ws !== null) {
            const {cols, rows} = fitAddon.proposeDimensions()!;
            ws.send(JSON.stringify({action: "resize", cols, rows: rows}));
            fitAddon.fit();
        }
    });
});

// 这个函数在sz命令下载文件的时候用的到，也是源码写好的，可以直接用
function _save_to_disk(packets, name) {
    // const blob = new Blob(packets);
    const url = URL.createObjectURL(new Blob(packets));
    const el = document.createElement("a");
    el.style.display = "none";
    el.href = url;
    el.download = name;
    document.body.appendChild(el);
    //It seems like a security problem that this actually works;
    //I’d think there would need to be some confirmation before
    //a browser could save arbitrarily many bytes onto the disk.
    //But, hey.
    el.click();
    document.body.removeChild(el);
}

// * ============================== ↓ get params in url ↓ ============================== * //
function getUrlParam(name: string) {
    const urlparam = new URLSearchParams(window.location.search);
    return urlparam.get(name);
}

function getWsUri() {
    const host = `${ip.value}:${port.value}`;
    if (!isTunnel) return `ws://${host}/ws`;
    const path = getUrlParam("path") ?? "ws";
    const _targetServer = getUrlParam("targetServer");
    let protocol = location.protocol === "https:" ? "wss://" : "ws://";
    const uri = `${protocol}${host}/${encodeURIComponent(path)}?method=connectArthas&id=${agentID.value}`;
    if (_targetServer != null) {
        return uri + "&targetServer=" + encodeURIComponent(_targetServer);
    }
    return uri;
}

function isBinaryEquals(a: ArrayBuffer, b: Int8Array) {
    if (a == null || b == null) {
        return false;
    }
    if (a.byteLength != b.byteLength) {
        return false;
    }
    let aView = new DataView(a);
    // let bView = new DataView(b);
    for (let i = 0; i < a.byteLength; i++) {
        if (aView.getInt8(i) != b.at(i)) {
            return false;
        }
    }
    return true;
}

class EventContext {
    data: Blob;
    inDownloadProgress: boolean;
    inUploadSendFileInfo: boolean;
    inUploadProgress: boolean;
    fileCache: Array<Blob>;
    fileCount: number;
    fileSize: number;
    fileName: string;


    constructor(data: Blob = null, inDownloadProgress: boolean = false, inUploadProgress: boolean = false, inUploadSendFileInfo: boolean = false, fileCache: Array<Blob> = [], fileCount: number = 0, fileSize: number = 0, fileName: string = null) {
        this.data = data;
        this.inDownloadProgress = inDownloadProgress;
        this.inUploadSendFileInfo = inUploadSendFileInfo;
        this.inUploadProgress = inUploadProgress;
        this.fileCache = fileCache;
        this.fileCount = fileCount;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.resetElement();
    }

    resetElement(){
        let elementById = document.getElementById("file");
        if (elementById != null) {
            elementById.value = null;
        }
    }

    startDownload() {
        this.inDownloadProgress = true;
    }

    startUpload() {
        if (this.inUploadSendFileInfo) {
            this.inUploadProgress = true;
        } else {
            this.inUploadSendFileInfo = true;
        }
    }


    setData(value: Blob) {
        this.data = value;
        return this;
    }
}

function isEventSize(blob: Blob) {
    return blob.size == START_DOWNLOAD_EVENT.byteLength
        || blob.size == START_UPLOAD_EVENT.byteLength
        || blob.size == CONFIRM_EVENT.byteLength
        || blob.size == CANCEL_EVENT.byteLength
        || blob.size == FAIL_EVENT.byteLength
        || blob.size == END_EVENT.byteLength;
}

async function eventHandler(eventContext: EventContext) {
    let arrayBuffer = await new Response(eventContext.data).arrayBuffer();
    let nowType = "";
    if (eventContext.inDownloadProgress) {
        nowType = "下载";
    } else if (eventContext.inUploadProgress || eventContext.inUploadSendFileInfo) {
        nowType = "上传";
    }
    // start download
    if (isBinaryEquals(arrayBuffer, START_DOWNLOAD_EVENT)) {
        eventContext.startDownload();
        // start upload
    } else if (isBinaryEquals(arrayBuffer, START_UPLOAD_EVENT)) {
        eventContext.startUpload();
        if (eventContext.inUploadSendFileInfo && !eventContext.inUploadProgress) {
            chooseFile();
        }
        if (eventContext.inUploadProgress) {
            uploadProgress();
        }
        // confirm
    } else if (isBinaryEquals(arrayBuffer, CONFIRM_EVENT)) {
        xterm.write("\n文件已存在是否覆盖(Y/N)?\n");
        // cancel
    } else if (isBinaryEquals(arrayBuffer, CANCEL_EVENT)) {
        eventContext = new EventContext();
        xterm.write("\n" + nowType + "已取消\n");
        // fail
    } else if (isBinaryEquals(arrayBuffer, FAIL_EVENT)) {
        eventContext = new EventContext();
        xterm.write("\n" + nowType + "失败\n");
        //end
    } else if (isBinaryEquals(arrayBuffer, END_EVENT)) {
        if (eventContext.inDownloadProgress) {
            _save_to_disk(eventContext.fileCache, eventContext.fileName);
        }
        eventContext = new EventContext();
        xterm.write("\n" + nowType + "成功\n");
    }else if (eventContext.inDownloadProgress) {
        if (eventContext.fileName == null) {
            return assembleDownloadFileInfo(eventContext);
        }
        return downloadProgress(eventContext);
    }
    return eventContext;
}

async function assembleDownloadFileInfo(eventContext: EventContext) {
    let arrayBuffer = await new Response(eventContext.data).arrayBuffer();
    // fileSize::fileName
    const text = decoder.decode(arrayBuffer);
    let strings = text.split('::');
    eventContext.fileSize = parseInt(strings[0]);
    eventContext.fileName = strings[1];
    if (Number.isNaN(eventContext.fileSize)) {
        throw new Error("文件长度不正确");
    }
    if (eventContext.fileName == "" || eventContext.fileName == null) {
        throw new Error("文件名不正确");
    }
    return eventContext;
}

function downloadProgress(eventContext: EventContext) {
    eventContext.fileCache.push(eventContext.data);
    eventContext.fileCount += eventContext.data.size;
    let precent = (eventContext.fileCount * 100) / eventContext.fileSize;
    xterm.write('\r' + precent.toFixed(2) + '%');
    return eventContext;
}

function chooseFile() {
    let fileElement = document.getElementById("file");
    fileElement.click();
    fileElement.onchange = function (arg) {
        let file = arg.target.files[0];
        if (file != null) {
            ws?.send(JSON.stringify({action: "read", data: file.name + "::" + file.size}));
        }
    };
}

function uploadProgress() {
    let fileElement = document.getElementById("file");
    let reader = new FileReader();
    reader.onload = function (event) {
        let arrayBuffer = event.target.result;
        let blob = new Blob([arrayBuffer]);
        ws.send(blob);
    }
    reader.readAsArrayBuffer(fileElement.files[0]);
}

let continuePromise = Promise.resolve(new EventContext());


async function downloadResultAsync(eventContext): Promise<EventContext> {
    if (eventContext == null) {
        return new EventContext();
    }
    if (eventContext.data == null) {
        return eventContext;
    }
    if (isEventSize(eventContext.data)) {
        return eventHandler(eventContext);
    }
    if (eventContext.inDownloadProgress) {
        if (eventContext.fileName == null) {
            return assembleDownloadFileInfo(eventContext);
        }
        return downloadProgress(eventContext);
    }
}

function lszrzModePromise(data: Blob) {
    continuePromise = continuePromise.then(eventContext => downloadResultAsync(eventContext.setData(data)), err => new EventContext());
}


// * ============================== ↓ init websocket ↓ ============================== * //
function initWs(silent: boolean) {
    let uri = getWsUri();
    ws = new WebSocket(uri);
    ws.onerror = function () {
        ws?.close();
        ws = undefined;
        !silent && alert("Connect error");
    };
    ws.onopen = function () {
        fullSc.value = true;

        let scrollback = getUrlParam("scrollback") ?? "0";

        const {cols, rows} = initXterm(scrollback);
        xterm.onData(function (data) {
            ws?.send(JSON.stringify({action: "read", data: data}));
        });

        ws!.onmessage = function (event: MessageEvent) {
            if (event.type === "message") {
                if (event.data instanceof Blob) {
                    lszrzModePromise(event.data);
                } else {
                    xterm.write(event.data);
                }
            }
        };

        ws?.send(JSON.stringify({action: "resize", cols, rows}));

        intervalReadKey = window.setInterval(function () {
            if (ws != null && ws.readyState === 1) {
                ws.send(JSON.stringify({action: "read", data: ""}));
            }
        }, 30000);
    };

    ws.onclose = function (message) {
        if (intervalReadKey != -1) {
            window.clearInterval(intervalReadKey);
            intervalReadKey = -1;
        }
        if (message.code === 2000) {
            alert(message.reason);
        }
    };
}

// * ============================== ↓ init xterm ↓ ============================== * //
function initXterm(scrollback: string) {
    let scrollNumber = parseInt(scrollback, 10);
    xterm = new Terminal({
        screenReaderMode: false,
        convertEol: true,
        allowProposedApi: true,
        scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK
    });
    xterm.loadAddon(fitAddon)

    xterm.open(document.getElementById('terminal')!);

    xterm.loadAddon(webglAddon)
    fitAddon.fit()
    return {
        cols: xterm.cols,
        rows: xterm.rows
    }
}

function isValidNumber(scrollNumber: number) {
    return scrollNumber >= MIN_SCROLL_BACK &&
        scrollNumber <= MAX_SCROLL_BACK;
}

const connectGuard = (silent: boolean): boolean => {
    if (ip.value.trim() === "" || port.value.trim() === "") {
        alert("Ip or port can not be empty");
        return false;
    }
    if (isTunnel && agentID.value == "") {
        if (silent) {
            return false;
        }
        alert("AgentId can not be empty");
        return false;
    }
    if (ws) {
        alert("Already connected");
        return false;
    }
    return true;
};

// * ============================== ↓ begin connect ↓ ============================== * //
function startConnect(silent: boolean = false) {
    if (connectGuard(silent)) {
        // init webSocket
        initWs(silent);
    }

}

function disconnect() {
    try {
        ws!.close();
        ws!.onmessage = null;
        ws!.onclose = null;
        ws = undefined;
        xterm.dispose();
        fitAddon.dispose();
        webglAddon.dispose();
        fullSc.value = false;
        alert("Connection was closed successfully!");
    } catch {
        alert("No connection, please start connect first.");
    }
}

// * ============================== ↓ full screen show ↓ ============================== * //
function xtermFullScreen() {
    const ele = document.getElementById("terminal-card")!;
    requestFullScreen(ele);
    ele.onfullscreenchange = (e: Event) => {
        fitAddon.fit();
    };
}

function requestFullScreen(element: HTMLElement) {
    let requestMethod = element.requestFullscreen;
    if (requestMethod) {
        requestMethod.call(element);
        //@ts-ignore
    } else if (window.ActiveXObject) {
        // @ts-ignore
        var wscript = new ActiveXObject("WScript.Shell");
        if (wscript !== null) {
            wscript.SendKeys("{F11}");
        }
    }
}

</script>

<template>
    <div class="flex flex-col h-[100vh] w-[100vw] resize-none">
        <nav v-if="iframe" class="navbar bg-base-100 md:flex-row flex-col w-[100vw]">
            <div class="navbar-start">
                <div class="dropdown dropdown-start 2xl:hidden">
                    <label tabindex="0" class="btn btn-ghost btn-sm">
                        <MenuAlt2Icon class="w-6 h-6"></MenuAlt2Icon>
                    </label>
                    <ul tabindex="0" class="dropdown-content menu shadow bg-base-100">
                        <li>
                            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                               href="https://arthas.aliyun.com/doc"
                               target="_blank">Documentation
                                <span class="sr-only">(current)</span></a>
                        </li>
                        <li>
                            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                               href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
                                Tutorials</a>
                        </li>
                        <li>
                            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                               href="https://github.com/alibaba/arthas"
                               target="_blank">Github</a>
                        </li>
                    </ul>
                </div>
                <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="mr-2 w-20"><img
                    :src="arthasLogo" alt="Arthas" title="Welcome to Arthas web console"></a>

                <ul class="menu menu-vertical 2xl:menu-horizontal hidden">
                    <li>
                        <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                           href="https://arthas.aliyun.com/doc"
                           target="_blank">Documentation
                            <span class="sr-only">(current)</span></a>
                    </li>
                    <li>
                        <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                           href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
                            Tutorials</a>
                    </li>
                    <li>
                        <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                           href="https://github.com/alibaba/arthas"
                           target="_blank">Github</a>
                    </li>
                </ul>

            </div>
            <div class="navbar-center ">
                <div class=" xl:flex-row form-control"
                     :class="{
          'xl:flex-row':isTunnel,
          'lg:flex-row':!isTunnel
        }">
                    <label class="input-group input-group-sm mr-2">
                        <span>IP</span>
                        <input type="text" placeholder="please enter ip address" class="input input-bordered input-sm "
                               v-model="ip"/>
                    </label>
                    <label class="input-group input-group-sm mr-2">
                        <span>Port</span>
                        <input type="text" placeholder="please enter port" class="input input-sm input-bordered"
                               v-model="port"/>
                    </label>
                    <label v-if="isTunnel" class="input-group input-group-sm mr-2">
                        <span>AgentId</span>
                        <input type="text" placeholder="please enter AgentId" class="input input-sm input-bordered"
                               v-model="agentID"/>
                    </label>
                </div>
            </div>
            <div class="navbar-end">
                <div class="btn-group   2xl:btn-group-horizontal btn-group-horizontal"
                     :class="{
          'md:btn-group-vertical':isTunnel
        }">
                    <button
                        class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
                        @click.prevent="startConnect(true)">Connect
                    </button>
                    <button
                        class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
                        @click.prevent="disconnect">Disconnect
                    </button>
                    <a class="btn btn-sm bg-secondary hover:bg-secondary-focus border-none text-secondary-content focus:bg-secondary-focus normal-case"
                       :href="outputHerf" target="_blank">Arthas Output</a>
                </div>
            </div>
        </nav>
        <div class="w-full h-0 flex-auto bg-black overscroll-auto" id="terminal-card">
            <div id="terminal" class="w-full h-full"></div>
        </div>

        <div title="fullscreen" id="fullSc" class="fullSc" v-if="fullSc">
            <button id="fullScBtn" @click="xtermFullScreen"><img :src="fullPic"></button>
        </div>

        <input type="file" id="file" style="display: none">
    </div>
</template>

<style>
#terminal:-webkit-full-screen {
    background-color: rgb(255, 255, 12);
}

.fullSc {
    z-index: 10000;
    position: fixed;
    top: 25%;
    left: 90%;
}

#fullScBtn {
    border-radius: 17px;
    border: 0;
    cursor: pointer;
    background-color: black;
}
</style>
