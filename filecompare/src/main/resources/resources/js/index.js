var value, orig1, orig2, dv, panes = 2, highlight = true, connect = null, collapse = false;

$(function(){
    //showDetail();
    $("input[type=file]").change(function(){
       var id = $(this).attr("id");
       upload(id);
    });
});

function upload(id){
    var formData = new FormData();
    console.log($("#"+id)[0]);
    console.log($("#"+id)[0].files[0]);
    formData.append("file", $("#"+id)[0].files[0]);
    $.ajax({
        type : "post",
        url : "/compare/upload",
        data : formData,
        cache : false,
        processData : false, // 不处理发送的数据，因为data值是Formdata对象，不需要对数据做处理
        contentType : false, // 不设置Content-type请求头
        success : function(data){
            if(data.status == "ok"){

                $("#"+id+"_info").html("上传成功");
                $("#"+id+"_name").val(data.desc);

                var path1 = $("#file1_name").val();
                var path2 = $("#file2_name").val();

                if(path1 != "" && path2 != ""){
                    showDetail(path1, path2);
                }
            } else{
                alert(data.desc);
            }
        },
        error : function(e){
            alert("上传失败\n"+e);
        }
    })
}

//获取对应文件
function showDetail(path1, path2){
    $.ajax({
        url:"/compare/getFile",
        type:"POST",
        dataType:"json",
        cache:false,
        data:{path1: path1, path2: path2},
        async:true,
        success:function(result,status){
            if(result.status=="OK"){
                compareRule(result);
            }else{
                alert("加载失败");
            }
        }
    });
}

function compareRule(obj){
    var configure=obj.t1;
    var baseConf=obj.t2;

    value=configure;
    orig1=configure;
    orig2=baseConf;

    initUI();
}

function initUI() {
    if (value == null) return;
    var target = document.getElementById("view");
    target.innerHTML = "";

    dv = CodeMirror.MergeView(target, {
        value: value,
        origLeft: null,
        //origLeft: panes == 3 && !collapse && !connect ? orig1 : null,
        orig: orig2,
        lineNumbers: true,
        mode: "",
        //mode: "text/html",
        highlightDifferences: highlight,
        //connect: "align",
        connect: connect,
        //collapseIdentical: true
        collapseIdentical: collapse
    });
    console.log(dv);
    //toggleDifferences();
    //resize(dv);
}

function toggleDifferences() {
    dv.setShowDifferences(highlight = !highlight);
}

function resize(mergeView) {
    console.log(mergeView);
    var height = mergeViewHeight(mergeView);

    for(;;) {
        if (mergeView.leftOriginal())
            mergeView.leftOriginal().setSize(null, height);
        mergeView.editor().setSize(null, height);
        if (mergeView.rightOriginal())
            mergeView.rightOriginal().setSize(null, height);

        var newHeight = mergeViewHeight(mergeView);
        if (newHeight >= height) break;
        else height = newHeight;
    }
    mergeView.wrap.style.height = height + "px";
}