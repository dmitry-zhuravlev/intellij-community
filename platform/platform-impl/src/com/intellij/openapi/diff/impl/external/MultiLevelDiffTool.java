/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.diff.impl.external;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.*;
import com.intellij.openapi.diff.impl.CompositeDiffPanel;
import com.intellij.openapi.diff.impl.DiffUtil;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.config.AbstractProperty;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class MultiLevelDiffTool implements DiffTool, DiscloseMultiRequest {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.impl.external.MultiLevelDiffTool");
  private final List<DiffTool> myTools;

  public MultiLevelDiffTool(final List<DiffTool> tools) {
    myTools = tools;
  }

  @Override
  public void show(DiffRequest request) {
    Collection hints = request.getHints();
    boolean shouldOpenDialog = FrameDiffTool.shouldOpenDialog(hints);
    if (shouldOpenDialog) {
      final DialogBuilder builder = new DialogBuilder(request.getProject());
      final CompositeDiffPanel diffPanel = createPanel(request, builder.getWindow(), builder);
      if (diffPanel == null) {
        Disposer.dispose(builder);
        return;
      }
      final Runnable onOkRunnable = request.getOnOkRunnable();
      if (onOkRunnable != null){
        builder.setOkOperation(() -> {
          builder.getDialogWrapper().close(0);
          onOkRunnable.run();
        });
      } else {
        builder.removeAllActions();
      }
      builder.setCenterPanel(diffPanel.getComponent());
      builder.setPreferredFocusComponent(diffPanel.getPreferredFocusedComponent());
      builder.setTitle(request.getWindowTitle());
      builder.setDimensionServiceKey(request.getGroupKey());

      new AnAction() {
        public void actionPerformed(final AnActionEvent e) {
          builder.getDialogWrapper().close(0);
        }
      }.registerCustomShortcutSet(getActiveKeymapShortcuts("CloseContent"), diffPanel.getComponent());
      diffPanel.setDiffRequest(request);
      FrameDiffTool.showDiffDialog(builder, hints);
    } else {
      final FrameWrapper frameWrapper = new FrameWrapper(request.getProject(), request.getGroupKey());
      final CompositeDiffPanel diffPanel = createPanel(request, frameWrapper.getFrame(), frameWrapper);
      if (diffPanel == null) {
        Disposer.dispose(frameWrapper);
        return;
      }
      frameWrapper.setTitle(request.getWindowTitle());
      diffPanel.setDiffRequest(request);
      DiffUtil.initDiffFrame(request.getProject(), frameWrapper, diffPanel, diffPanel.getComponent());

      new AnAction() {
        public void actionPerformed(final AnActionEvent e) {
          Disposer.dispose(frameWrapper);
        }
      }.registerCustomShortcutSet(getActiveKeymapShortcuts("CloseContent"), diffPanel.getComponent());

      frameWrapper.show();
    }
  }

  private CompositeDiffPanel createPanel(final DiffRequest request, final Window window, @NotNull Disposable parentDisposable) {
    return new CompositeDiffPanel(request.getProject(), this, window, parentDisposable);
  }

  public DiffViewer viewerForRequest(Window window,
                                     @NotNull Disposable parentDisposable,
                                      final String name, DiffRequest current) {
    DiffViewer viewer = null;
    for (DiffTool tool : myTools) {
      if (tool.canShow(current)) {
        viewer = tool.createComponent(name, current, window, parentDisposable);
        if (viewer != null) {
          break;
        }
      }
    }
    return viewer;
  }

  @Override
  public boolean canShow(DiffRequest request) {
    return canShowRequest(request);
    //return request.haveMultipleLayers();
  }

  public static boolean canShowRequest(DiffRequest request) {
    boolean isFile = false;
    DiffContent[] contents = request.getContents();
    for (int i = 0; i < contents.length; i++) {
      DiffContent content = contents[i];
      VirtualFile file = content.getFile();
      if (file != null && file.isInLocalFileSystem() && ! file.isDirectory()) {
        isFile = true;
        break;
      }
    }
    AbstractProperty.AbstractPropertyContainer config = DiffManagerImpl.getInstanceEx().getProperties();
    if (isFile && DiffManagerImpl.ENABLE_FILES.value(config)) return false;
    if (! isFile && DiffManagerImpl.ENABLE_FOLDERS.value(config)) return false;
    return ! (DiffViewerType.merge.equals(request.getType()) && contentsWriteable(request));
  }

  private static boolean contentsWriteable(DiffRequest request) {
    final DiffContent[] contents = request.getContents();

    for (DiffContent content : contents) {
      if (content != null && content.getDocument().isWritable()) return true;
    }
    return false;
  }

  @Override
  public DiffViewer createComponent(String title, DiffRequest request, Window window, @NotNull Disposable parentDisposable) {
    // should not be called for it
    throw new IllegalStateException();
  }
}
