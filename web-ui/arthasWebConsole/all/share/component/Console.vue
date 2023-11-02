<script setup lang="ts">
import { MenuAlt2Icon } from "@heroicons/vue/outline";
import { ElNotification } from "element-plus";
import { computed, onMounted, ref } from "vue";
import { ITerminalOptions, Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import { Unicode11Addon } from "xterm-addon-unicode11";
import { WebglAddon } from "xterm-addon-webgl";
import "xterm/css/xterm.css"; // 这个css样式必须要引入，不然生成的terminal终端会有问题
import Zmodem from "zmodem.js";
import arthasLogo from "~/assert/arthas.png";
import fullPic from "~/assert/fullsc.png";

const { isTunnel = false } = defineProps<{
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

const uploadDialogVisible = ref(false);
const uploadLoading = ref(false);

const downloadDialogVisible = ref(false);
const percentage = ref(0);

const terminalSocket = ref<WebSocket | null>(null);

const outputHerf = computed(() => {
  console.log(agentID.value);
  return isTunnel ? `proxy/${ agentID.value }/arthas-output/` : `/arthas-output/`;
});

// const isTunnel = import.meta.env.MODE === 'tunnel'
const unicode11Addon = new Unicode11Addon();
const fitAddon = new FitAddon();
const webglAddon = new WebglAddon();
let xterm = new Terminal({ allowProposedApi: true });

const zsentry = ref(Zmodem.Sentry);
const zsession = ref(null);

const elUpload = ref()

onMounted(() => {
  ip.value = getUrlParam("ip") ?? window.location.hostname;
  port.value = getUrlParam("port") ?? ARTHAS_PORT;
  if (isTunnel) agentID.value = getUrlParam("agentId") ?? "";
  let _iframe = getUrlParam("iframe");
  if (_iframe && _iframe.trim() !== "false") iframe.value = false;

  startConnect(true);
  window.addEventListener("resize", function () {
    if (ws !== undefined && ws !== null) {
      const { cols, rows } = fitAddon.proposeDimensions()!;
      ws.send(JSON.stringify({ action: "resize", cols, rows: rows }));
      fitAddon.fit();
    }
  });
});

// * ============================== ↓ 增加 ↓ ============================== * //
// 这个方法地址：https://github.com/FGasper/zmodemjs/blob/master/src/zmodem_browser.js
function _send_files(session, files, options) {
  if (!options) options = {};
  //Populate the batch in reverse order to simplify sending
  //the remaining files/bytes components.
  const batch = [];
  let total_size = 0;
  for (let f = files.length - 1; f >= 0; f--) {
    const fobj = files[f];
    total_size += fobj.size;
    batch[f] = {
      obj: fobj, name: fobj.name, size: fobj.size, mtime: new Date(fobj.lastModified),
      files_remaining: files.length - f, bytes_remaining: total_size,
    };
  }
  let file_idx = 0;

  function promise_callback() {
    const cur_b = batch[file_idx];
    if (!cur_b) {
      return Promise.resolve(); //batch done!
    }
    file_idx++;
    return session.send_offer(cur_b).then(function after_send_offer(xfer) {
      if (options.on_offer_response) {
        options.on_offer_response(cur_b.obj, xfer);
      }
      if (xfer === undefined) {
        return promise_callback();   //skipped
      }
      return new Promise(function (res) {
        const reader = new FileReader();
        //This really shouldn’t happen … so let’s
        //blow up if it does.
        reader.onerror = function reader_onerror(e) {
          console.error("file read error", e);
          throw ("File read error: " + e);
        };

        let piece;
        reader.onprogress = function reader_onprogress(e) {
          //Some browsers (e.g., Chrome) give partial returns,
          //while others (e.g., Firefox) don’t.
          if (e.target.result) {
            piece = new Uint8Array(e.target.result, xfer.get_offset());
            // _check_aborted(session);
            if (session.aborted()) {
              throw new Zmodem.Error("aborted");
            }
            xfer.send(piece);
            if (options.on_progress) {
              options.on_progress(cur_b.obj, xfer, piece);
            }
          }
        };
        reader.onload = function reader_onload(e) {
          piece = new Uint8Array(e.target.result, xfer, piece);
          // _check_aborted(session);
          if (session.aborted()) {
            throw new Zmodem.Error("aborted");
          }
          xfer.end(piece).then(function () {
            if (options.on_progress && piece.length) {
              options.on_progress(cur_b.obj, xfer, piece);
            }
            if (options.on_file_complete) {
              options.on_file_complete(cur_b.obj, xfer);
            }
            //Resolve the current file-send promise with
            //another promise. That promise resolves immediately
            //if we’re done, or with another file-send promise
            //if there’s more to send.
            res(promise_callback());
          });
        };
        reader.readAsArrayBuffer(cur_b.obj);
      });
    });
  }

  return promise_callback();
}

function upload() {
  let fileElem: any = document.getElementsByName("file")[0];
  if (fileElem.files.length > 0) {
    uploadLoading.value = true;
    const _t = this;
    // 这里就需要用到_send_files函数，函数在下面，里面的逻辑不用动
    // 这个内置函数需要传三个参数，具体参数介绍在git里面有，不做赘述
    // 第三个参数是一个object，包含三个回调函数，可以自己拎出来
    _send_files(zsession.value, fileElem.files, {
      // 上传响应
      on_offer_response(obj, xfer) {
        // 如果回调参数xfer为undefined，说明上传有问题
        if (xfer) {
          console.log(xfer);
        } else {
          ElNotification({
            title: "Error",
            message: `${ obj.name } was upload skipped`,
            type: "error",
          });
        }
      },
      // 上传进度回调
      on_progress(obj, xfer) {
        let detail = xfer.get_details();
        let name = detail.name;
        let total = detail.size;
        let percent;
        if (total === 0) {
          percent = 100;
        } else {
          percent = Math.round((xfer.file_offset / total) * 100);
        }
        console.log(`${ percent }%`);
      },
      // 上传成功回调
      on_file_complete(obj) {
        ElNotification({
          title: "success",
          message: `${ obj.name } upload success`,
          type: "success",
        });
      },
    }).then(() => {
      fileElem.value = "";
      zsession.value.close();
      uploadDialogVisible.value = false;
      uploadLoading.value = false;
      terminalSocket.value.send("\n");
      xterm.focus();
    });
  } else {
    ElMessage({
      type: "error",
      message: "请选择文件",
    });
    uploadLoading.value = false;
  }
  elUpload.value?.clearFiles();
}

// 上传文件弹框关闭
function handleCloseUpload() {
  if (uploadLoading.value) {
    ElMessage({
      type: "error",
      message: "上传中无法关闭",
    });
  } else {
    zsession.value.close().then(() => {
      elUpload.value?.clearFiles();
    });
  }
}

// rzsz上传下载需要在这个内置函数中做一些改动
// 用来控制上传下载dialog弹框的显隐和upload方法的触发
function _on_detect(detection) {
  //Do this if we determine that what looked like a ZMODEM session
  //is actually not meant to be ZMODEM.
  zsession.value = detection.confirm();
  // 这里是监听上传事件
  if (zsession.type === "send") {
    //Send a group of files, e.g., from an <input>’s “.files”.
    //There are events you can listen for here as well,
    //e.g., to update a progress meter.
    // Zmodem.Browser.send_files( zsession, files_obj );

    // 打开上传dialog弹框
    uploadDialogVisible.value = true;
  }
  // 这里监听下载事件
  else {
    zsession.on("offer", (xfer) => {
      //Do this if you don’t want the offered file.

      // 这里是做了一个进度的计算，可有可无
      let total = xfer.get_details().bytes_remaining;
      let length = 0;
      downloadDialogVisible.value = true;
      xfer.on("input", (octets) => {
        length += octets.length;
        percentage.value = Math.ceil((length * 100) / total);
      });

      // 这里往下是功能区
      xfer.accept().then(() => {
        //Now you need some mechanism to save the file.
        //An example of how you can do this in a browser:'

        // 这个下载函数也是内置源码有的，在下面有，可以直接用
        _save_to_disk(
            xfer._spool,
            xfer.get_details().name,
        );
      });
    });
    // 监听到下载完毕，关闭下载弹框
    zsession.value.on("session_end", () => {
      percentage.value = 0;
      downloadDialogVisible.value = false;
    });
    zsession.value.start();
  }
}

// 这个函数在sz命令下载文件的时候用的到，也是源码写好的，可以直接用
function _save_to_disk(packets, name) {
  const blob = new Blob(packets);
  const url = URL.createObjectURL(blob);
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

function _to_terminal(octets) {
  // i.e. send to the ZMODEM peer
  if (terminalSocket.value) {
    terminalSocket.value.send(new Uint8Array(octets).buffer);
  }
}

function _on_retract() {
  //for when Sentry retracts a Detection
  console.log("retract");
}

function _sender(octets) {
  //i.e. send to the ZMODEM peer
  if (terminalSocket.value) {
    terminalSocket.value.send(new Uint8Array(octets).buffer);
  }
}

// * ============================== ↓ get params in url ↓ ============================== * //
function getUrlParam(name: string) {
  const urlparam = new URLSearchParams(window.location.search);
  return urlparam.get(name);
}

function getWsUri() {
  const host = `${ ip.value }:${ port.value }`;
  if (!isTunnel) return `ws://${ host }/ws`;
  const path = getUrlParam("path") ?? "ws";
  const _targetServer = getUrlParam("targetServer");
  let protocol = location.protocol === "https:" ? "wss://" : "ws://";
  const uri = `${ protocol }${ host }/${ encodeURIComponent(path) }?method=connectArthas&id=${ agentID.value }`;
  if (_targetServer != null) {
    return uri + "&targetServer=" + encodeURIComponent(_targetServer);
  }
  return uri;
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

    const { cols, rows } = initXterm(scrollback);
    xterm.onData(function (data) {
      ws?.send(JSON.stringify({ action: "read", data: data }));
    });
    ws!.onmessage = function (event: MessageEvent) {
      if (event.type === "message") {
        const data = event.data;
        xterm.write(data);
      }
    };
    ws?.send(JSON.stringify({ action: "resize", cols, rows }));
    intervalReadKey = window.setInterval(function () {
      if (ws != null && ws.readyState === 1) {
        ws.send(JSON.stringify({ action: "read", data: "" }));
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
    scrollback: isValidNumber(scrollNumber) ? scrollNumber : DEFAULT_SCROLL_BACK,
    cursorBlink: true,  // 光标闪烁
    cursorStyle: "underline",  // 光标闪烁样式
    rendererType: "canvas"  // 渲染类型
    // theme: { background: "#1d242b", selection: "rgba(245, 108, 108, 0.5)" },  // 主题样式
    // fontFamily: 'Consolas, Menlo, Monaco, "Courier New", monospace',
    // scrollback: 10000
  } as ITerminalOptions);
  xterm.loadAddon(fitAddon);
  xterm.loadAddon(unicode11Addon);
  xterm.unicode.activeVersion = "11";
  xterm.onResize((size) => {
    console.log(size.rows, size.cols);
  });

  xterm.open(document.getElementById("terminal")!);

  xterm.loadAddon(webglAddon);
  fitAddon.fit();
  xterm.focus();
  // xterm.write("hello, welcome to terminal!");

  zsentry.value = new Zmodem.Sentry({
    // 这几个参数都为必要参数，是zmodem.js库源码示例中所必须的，每个参数都对应一个function
    // 为了代码可读，把相关方法单独拎了出来
    // 这几个内置函数在源码里都有，里面的逻辑有些小改动，可以对比查看
    to_terminal: _to_terminal,  //发送的处理程序 到终端对象的流量。接收可迭代对象（例如，数组）包含八位字节数。
    sender: _sender,  // 将流量发送到的处理程序对等方。例如，如果您的应用程序使用 WebSocket 进行通信到对等方，使用它将数据发送到 WebSocket 实例。
    on_detect: _on_detect,  // 处理程序检测事件。接收新的检测对象。
    on_retract: _on_retract,  // 于收回的处理程序事件。不接收任何输入。
  });

  return {
    cols: xterm.cols,
    rows: xterm.rows,
  };
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
    const wscript = new ActiveXObject("WScript.Shell");
    wscript && wscript?.SendKeys("{F11}");
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
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://arthas.aliyun.com/doc"
                 target="_blank">Documentation
                <span class="sr-only">(current)</span></a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
                 href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
                Tutorials</a>
            </li>
            <li>
              <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
                 target="_blank">Github</a>
            </li>
          </ul>
        </div>
        <a href="https://github.com/alibaba/arthas" target="_blank" title="" class="mr-2 w-20"><img
            :src="arthasLogo" alt="Arthas" title="Welcome to Arthas web console"></a>

        <ul class="menu menu-vertical 2xl:menu-horizontal hidden">
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://arthas.aliyun.com/doc"
               target="_blank">Documentation
              <span class="sr-only">(current)</span></a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm"
               href="https://arthas.aliyun.com/doc/arthas-tutorials.html" target="_blank">Online
              Tutorials</a>
          </li>
          <li>
            <a class="hover:text-sky-500 dark:hover:text-sky-400 text-sm" href="https://github.com/alibaba/arthas"
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
            <input type="text" placeholder="please enter port" class="input input-sm input-bordered" v-model="port"/>
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
      <button id="fullScBtn" @click="xtermFullScreen">
        <img :src="fullPic" alt="" />
      </button>
    </div>

    <!-- rz命令上传文件 -->
    <el-dialog title="请选择要上传的文件" v-model="uploadDialogVisible" width="400px" :before-close="handleCloseUpload">
      <el-upload ref="elUpload" action="http://localhost/posts/" multiple :auto-upload="false" v-loading="uploadLoading">
        <el-button slot="trigger" type="primary">选取文件</el-button>
        <el-button type="primary" style="margin-left: 10px" @click="upload">上传</el-button>
      </el-upload>
    </el-dialog>

    <!-- sz命令下载文件 -->
    <el-dialog title="正在下载请稍后" v-model="downloadDialogVisible" width="400px">
      <el-progress :percentage="percentage"/>
    </el-dialog>
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
