package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LsModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;

/**
 * @author gongdewei 2020/4/3
 */
public class LsView extends ResultView<LsModel> {

    @Override
    public void draw(CommandProcess process, LsModel result) {
        if (result.getList() == null || result.getList().length == 0) {
            process.write("\n");
        } else {
            process.write(RenderUtil.render(create(result.getList()), process.width()) + "\n");
        }
    }

    private static Element create(String[] list) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        RowElement row = null;
        for (int i = 0; i < list.length; i++) {
            if (i  % 6 == 0) {
                row = row();
                table.add(row);
            }
            row.add(label(list[i]).style(Style.style(Color.red)));
        }
        if (row!=null) {
            for (int i = row.getSize(); i < 6; i++) {
                row.add(label(""));
            }
        }
        return table;
    }

    public static void main(String[] args) {
        String[] list = new File("D:\\IdeaProjects\\arthas\\core\\src\\main\\java\\com\\taobao\\arthas\\core\\command\\model").list();
        Element element = create(list);
        System.out.println(RenderUtil.render(element, 512));
    }
}
