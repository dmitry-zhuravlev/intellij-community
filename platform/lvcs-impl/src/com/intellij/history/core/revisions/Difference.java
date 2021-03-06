/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.history.core.revisions;

import com.intellij.history.core.tree.Entry;
import com.intellij.history.integration.IdeaGateway;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ByteBackedContentRevision;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class Difference {
  private final boolean myIsFile;
  private final Entry myLeft;
  private final Entry myRight;
  private final boolean myRightContentCurrent;

  public Difference(boolean isFile, Entry left, Entry right) {
    this(isFile, left, right, false);
  }

  public Difference(boolean isFile, @Nullable Entry left, @Nullable Entry right, boolean isRightContentCurrent) {
    myIsFile = isFile;
    myLeft = left;
    myRight = right;
    myRightContentCurrent = isRightContentCurrent;
  }

  public boolean isFile() {
    return myIsFile;
  }

  public Entry getLeft() {
    return myLeft;
  }

  public Entry getRight() {
    return myRight;
  }

  public ContentRevision getLeftContentRevision(IdeaGateway gw) {
    return createContentRevision(getLeft(), gw);
  }

  public ContentRevision getRightContentRevision(IdeaGateway gw) {
    Entry entry = getRight();
    if (myRightContentCurrent && entry != null) {
      VirtualFile file = gw.findVirtualFile(entry.getPath());
      if (file != null) return new CurrentContentRevision(VcsUtil.getFilePath(file));
    }
    return createContentRevision(entry, gw);
  }

  private static ContentRevision createContentRevision(@Nullable Entry e, final IdeaGateway gw) {
    if (e == null) return null;

    return new ByteBackedContentRevision() {
      @Nullable
      public String getContent() throws VcsException {
        if (e.isDirectory()) return null;
        return e.getContent().getString(e, gw);
      }

      @Nullable
      @Override
      public byte[] getContentAsBytes() throws VcsException {
        if (e.isDirectory()) return null;
        return e.getContent().getBytes();
      }

      @NotNull
      public FilePath getFile() {
        return VcsUtil.getFilePath(new File(e.getPath()), e.isDirectory());
      }

      @NotNull
      public VcsRevisionNumber getRevisionNumber() {
        return VcsRevisionNumber.NULL;
      }
    };
  }
}
