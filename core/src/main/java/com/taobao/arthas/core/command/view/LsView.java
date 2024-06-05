package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LsModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;

/**
 * @author gongdewei 2020/4/3
 */
public class LsView extends ResultView<LsModel> {

    @Override
    public void draw(CommandProcess process, LsModel result) {
        if (result.getList() == null || result.getList().isEmpty()) {
            process.write("\n");
        } else {
            process.write(RenderUtil.render(create(result.getList(), process.width()), process.width()) + "\n");
        }
    }

    private static Element create(List<LsModel.FileNode> list, int width) {
        int maxCellWidth = list.stream().map(LsModel.FileNode::getName).mapToInt(String::length).max().orElse(1);
        int num = width / maxCellWidth;
        num = num == 0 ? 1 : num;
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        RowElement row = null;
        for (int i = 0; i < list.size(); i++) {
            if (i  % num == 0) {
                row = row();
                table.add(row);
            }
            row.add(label(list.get(i).getName()).style(Style.style(list.get(i).isDir() ? Color.blue : Color.white).bold(true)));
        }
        if (row!=null) {
            for (int i = row.getSize(); i < num; i++) {
                row.add(label(""));
            }
        }
        return table;
    }
}
