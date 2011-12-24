package tk.stepcounter.diffcount;

import java.io.File;

import tk.stepcounter.diffcount.diff.DiffEngine;
import tk.stepcounter.diffcount.diff.IDiffHandler;
import tk.stepcounter.diffcount.object.AbstractDiffResult;
import tk.stepcounter.diffcount.object.DiffFileResult;
import tk.stepcounter.diffcount.object.DiffFolderResult;
import tk.stepcounter.diffcount.object.DiffStatus;

/**
 * �����̃J�E���g�������s���܂��B
 * 
 */
public class DiffCounter {

	private static String	encoding	= null;

	public static void setEncoding(String encoding) {
		DiffCounter.encoding = encoding;
	}

	private static String getEncoding(File file) {
		if (encoding != null) {
			return encoding;
		}
		return DiffCounterUtil.getFileEncoding(file);
	}

	/**
	 * 2�̃f�B���N�g���z���̃\�[�X�R�[�h�̍������J�E���g���܂��B
	 *
	 * @param oldRoot �ύX�O�̃\�[�X�c���[�̃��[�g�f�B���N�g��
	 * @param newRoot �ύX��̃\�[�X�c���[�̃��[�g�f�B���N�g��
	 * @return �J�E���g����
	 */
	public static DiffFolderResult count(File oldRoot, File newRoot) {

		// TODO �t�@�C���P�ʂł̍����ɂ��Ή�����B

		DiffFolderResult root = new DiffFolderResult(null);
		root.setName(newRoot.getName());

		diffFolder(root, oldRoot, newRoot);

		return root;
	}

	private static void diffFolder(DiffFolderResult parent, File oldFolder,
			File newFolder) {
		File[] oldFiles = oldFolder.listFiles();
		if (oldFiles == null) {
			oldFiles = new File[0];
		}

		File[] newFiles = newFolder.listFiles();

		for (File newFile : newFiles) {
			if (DiffCounterUtil.isIgnore(newFile)) {
				continue;
			}

			boolean found = false;

			for (File oldFile : oldFiles) {
				if (newFile.getName().equals(oldFile.getName())) {
					AbstractDiffResult result = createDiffResult(parent,
							oldFile, newFile, getEncoding(newFile),
							DiffStatus.MODIFIED);
					if (result != null) {
						parent.addChild(result);
					}
					found = true;
					break;
				}
			}

			// �Â��\�[�X�c���[�Ɍ�����Ȃ������ꍇ�͒ǉ�
			if (found == false) {
				AbstractDiffResult result = createDiffResult(parent, null,
						newFile, getEncoding(newFile), DiffStatus.ADDED);
				parent.addChild(result);
			}

			// �f�B���N�g���̏ꍇ�͍ċA�I�ɏ���
			if (newFile.isDirectory()) {
				DiffFolderResult newParent = (DiffFolderResult)parent.getChildren().get(
						parent.getChildren().size() - 1);
				diffFolder(newParent, new File(oldFolder, newFile.getName()),
						newFile);
			} else {

			}
		}

		// �폜���ꂽ�t�H���_�𒊏o�B���܂킷�͔̂�����ł����c
		for (File oldFile : oldFiles) {
			if (DiffCounterUtil.isIgnore(oldFile)) {
				continue;
			}

			boolean found = false;

			for (File newFile : newFiles) {
				if (oldFile.getName().equals(newFile.getName())) {
					found = true;
					break;
				}
			}

			if (found == false) {
				parent.addChild(createDiffResult(parent, oldFile, null,
						getEncoding(oldFile), DiffStatus.REMOVED));
			}
		}

	}

	private static AbstractDiffResult createDiffResult(DiffFolderResult parent,
			File oldFile, File newFile, String charset, DiffStatus status) {

		if (newFile != null && newFile.isFile()) {
			DiffFileResult diffResult;

			Cutter cutter = CutterFactory.getCutter(newFile);
			if (cutter != null) {
				diffResult = createDiffFileResult(parent, oldFile, newFile,
						charset, status, cutter);
			} else {
				// �J�b�^�[���擾�ł��Ȃ������ꍇ�̓T�|�[�g�ΏۊO�Ƃ���
				diffResult = new DiffFileResult(parent);
				diffResult.setName(newFile.getName());
				diffResult.setStatus(DiffStatus.UNSUPPORTED);
				diffResult.setAddCount(0);
				diffResult.setFileType(CutterFactory.getFileType(newFile));
			}

			return diffResult;

		} else if (oldFile != null && oldFile.isFile()) {
			DiffFileResult diffResult = new DiffFileResult(parent);
			diffResult.setName(oldFile.getName());
			diffResult.setStatus(status);
			diffResult.setFileType(CutterFactory.getFileType(oldFile));

			Cutter cutter = CutterFactory.getCutter(oldFile);

			if (cutter != null && status == DiffStatus.REMOVED) {
				// �폜�t�@�C���̏ꍇ
				DiffSource source = cutter.cut(DiffCounterUtil.getSource(
						oldFile, charset));
				if (source.isIgnore()) {
					return null;
				}
				diffResult.setDelCount(DiffCounterUtil.split(source.getSource()).length);
				diffResult.setCategory(source.getCategory());
			}

			return diffResult;

		} else if (newFile != null && newFile.isDirectory()) {
			DiffFolderResult diffResult = new DiffFolderResult(parent);
			diffResult.setName(newFile.getName());
			diffResult.setStatus(status);
			return diffResult;

		} else if (oldFile != null && oldFile.isDirectory()) {
			DiffFolderResult diffResult = new DiffFolderResult(parent);
			diffResult.setName(oldFile.getName());
			diffResult.setStatus(status);
			return diffResult;
		}

		return null;
	}

	private static DiffFileResult createDiffFileResult(DiffFolderResult parent,
			File oldFile, File newFile, String charset, DiffStatus status,
			Cutter cutter) {

		DiffFileResult diffResult = new DiffFileResult(parent);

		diffResult.setFileType(CutterFactory.getFileType(newFile));
		diffResult.setName(newFile.getName());
		diffResult.setStatus(status);

		if (status == DiffStatus.ADDED) {
			// �V�K�t�@�C���̏ꍇ
			DiffSource source = cutter.cut(DiffCounterUtil.getSource(newFile,
					charset));
			if (source.isIgnore()) {
				return null;
			}
			diffResult.setAddCount(DiffCounterUtil.split(source.getSource()).length);
			diffResult.setCategory(source.getCategory());
		} else if (status == DiffStatus.MODIFIED) {
			// �ύX�t�@�C���̏ꍇ
			DiffSource oldSource = cutter.cut(DiffCounterUtil.getSource(
					oldFile, charset));
			DiffSource newSource = cutter.cut(DiffCounterUtil.getSource(
					newFile, charset));

			if (newSource.isIgnore()) {
				return null;
			}

			DiffCountHandler handler = new DiffCountHandler();
			DiffEngine engine = new DiffEngine(handler, oldSource.getSource(),
					newSource.getSource());
			engine.doDiff();

			diffResult.setAddCount(handler.getAddCount());
			diffResult.setDelCount(handler.getDelCount());
			diffResult.setCategory(newSource.getCategory());

			if (handler.getAddCount() == 0) {
				diffResult.setStatus(DiffStatus.NONE);
			}
		} else if (status == DiffStatus.REMOVED) {
			// �폜�t�@�C���̏ꍇ
			DiffSource source = cutter.cut(DiffCounterUtil.getSource(oldFile,
					charset));
			if (source.isIgnore()) {
				return null;
			}
			diffResult.setDelCount(DiffCounterUtil.split(source.getSource()).length);
			diffResult.setCategory(source.getCategory());
		} else {
			diffResult = null;
		}

		return diffResult;
	}

	/**
	 * �ύX�s�����J�E���g���邽�߂�{@link IDiffHandler}�����N���X�ł��B
	 */
	private static class DiffCountHandler implements IDiffHandler {

		/** �ǉ��s�� */
		private int	addCount	= 0;

		/** �폜�s�� */
		private int	delCount	= 0;

		/**
		 * {@inheritDoc}
		 */
		public void add(String text) {
			this.addCount++;
		}

		/**
		 * {@inheritDoc}
		 */
		public void delete(String text) {
			this.delCount++;
		}

		/**
		 * {@inheritDoc}
		 */
		public void match(String text) {}

		/**
		 * �ǉ��s�����擾���܂��B
		 * 
		 * @return �ǉ��s��
		 */
		public int getAddCount() {
			return this.addCount;
		}

		/**
		 * �폜�s�����擾���܂��B
		 * 
		 * @return �폜�s��
		 */
		public int getDelCount() {
			return this.delCount;
		}
	}

}