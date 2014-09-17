package ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import app.App;

public class ActionControlPanel extends JPanel implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7837368675776102944L;

	private JButton refresh_button;
	private JButton start_analysis;
	public JLabel selected_items ;
	private int selected_items_value;
	
	public ActionControlPanel() {
		super();
		buildContentPane();
		/*
		Label selected_items_label = new Label();
		Label refresh_metadata = new Label(AppLabels.APP_REFRESH_METADATA);
		Button refresh_button = new Button(AppLabels.APP_BUTTON_REFRESH);
		Button start_analysis = new Button(APP_BUTTON_ANALYSIS);
		*/
	}
	
	private void buildContentPane(){

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		selected_items = new JLabel(AppLabels.APP_SELECTED + 0);
		c.gridx = 0;
		c.gridwidth = 4;
		c.gridwidth = 4;
		c.insets = new Insets(10,10,10,10);
		this.add(selected_items, c);
		
		refresh_button = new JButton(AppLabels.APP_BUTTON_REFRESH);
		refresh_button.addActionListener(this);
		c.gridx = 2;
		c.gridy = 6;
		c.gridwidth = 2;
		c.insets = new Insets(10,10,10,10);
		this.add(refresh_button, c);
 
		start_analysis = new JButton(AppLabels.APP_BUTTON_ANALYSIS);
		start_analysis.addActionListener(this);
		c.gridx = 2;
		c.gridy = 8;
		c.gridwidth = 2;
		c.insets = new Insets(10,10,10,10);
		this.add(start_analysis, c);
	}
	
	public void setSelectedItems(int v){
		this.selected_items_value = v;
		this.selected_items.setText(AppLabels.APP_SELECTED + this.selected_items_value);
	}
 
	@Override
	public void actionPerformed(ActionEvent e) {
 		Object source = e.getSource();
		if(source == refresh_button){
			App.reload_metadata(MainWindow.getInstance().getSelectedTreePaths());
		}else if(source == start_analysis){
			App.launch_analysis(MainWindow.getInstance().getSelectedTreePaths());
		}
	}
}