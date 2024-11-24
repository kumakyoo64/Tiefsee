import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RTI
{
    public static int[] OFFSET = {-16,-16,-15,-15,-13,-13,-10,-10};

    public static String INPUT_FILENAME;
    public static int PROGRESS = 0;
    public static int USE_BRUTE_FORCE = 0;
    public static String dump = null;
    public static String outname = "output";

    public static void main(String[] args)
    {
        parse_args(args);

        RTI rti = new RTI();
        rti.read(INPUT_FILENAME);
        if (dump!=null)
            rti.read_dump(dump);

        rti.approx_with_sprites_and_chars();
        rti.save_dump();
        rti.write_output();
    }

    private static void parse_args(String[] args)
    {
        if (args.length==0)
        {
            System.err.println("Usage: java RTI [options] <rti-file>");
            System.err.println();
            System.err.println("  -p           Show progress (twice = more progress)");
            System.err.println("  -b           Use brute-force search instead of heuristics");
            System.err.println("  -o <file>    Output filename");
            System.err.println("  -d <dump>    Start with dump");
            System.exit(-1);
        }

        int pos = 0;
        while (pos<args.length-1)
        {
            String param = args[pos++];
            switch (param.substring(1))
            {
            case "p":
                PROGRESS++;
                break;
            case "b":
                USE_BRUTE_FORCE++;
                break;
            case "d":
                dump = args[pos++];
                break;
            case "o":
                outname = args[pos++];
                break;
            default:
                System.err.println("Unknown Parameter: "+param);
                System.exit(-1);
            }
        }

        INPUT_FILENAME = args[args.length-1];
    }

    //////////////////////////////////////////////////////////////////

    private Description descr;
    private Data data;
    private List<Switch> switches;

    private int[][] goal;
    private int[][] now;

    private Charset charset;

    private int[][] hr_sa;
    private int[][] mc_sa;

    private void read(String filename)
    {
        descr = new Description(filename);
        Sprite.HEIGHT = 8*descr.HEIGHT;
        goal = descr.get_goal();

        data = new Data(charwidth(),charheight());

        now = new int[width()][height()];
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
                now[i][j] = descr.BGCOL;

        charset = new Charset();
        switches = new ArrayList<>();

        for (int s=0;s<8;s++)
            for (int i=0;i<4;i++)
            {
                int y = 58+OFFSET[s]+21*(i+1);
                switches.add(new Switch(2040+s,Switch.INC,y,11,y,54));
                if (i!=0)
                    switches.add(new Switch(53249+2*s,y,y-20,11,y-1,54));
            }
    }

    private void write_output()
    {
        write_file(outname+".plain",write());
        write_file(outname+".sng",write_sng());
        write_file(outname+".a",write_acme());

//        System.err.println("Used characters: "+charset.charset_start+"-"+(charset.charset_end-1));
//        System.err.println(status());
//        System.err.println(sprite_status());
    }

    private void write_file(String filename, String content)
    {
        try {
            PrintWriter p = new PrintWriter(new FileOutputStream(new File(filename)));
            p.write(content);
            p.close();
        } catch (IOException e)
            {
                e.printStackTrace();
            }
    }

    private String write()
    {
        StringBuffer b = new StringBuffer((width()+1)*(height()+1));
        for (int j=0;j<height();j++)
        {
            for (int i=0;i<width();i++)
                b.append(Integer.toHexString(data.get_pixel(i,j,descr.BGCOL)).toUpperCase());
            b.append("\n");
        }
        return b.toString();
    }

    private String write_sng()
    {
        int[][] chr = calc_charset();

        String erg = "";
        erg += "#SNG\n";
        erg += "IHDR {\n";
        erg += "    width: "+(width())+"; height: "+(height())+"; bitdepth: 8;\n";
        erg += "    using color palette;\n";
        erg += "}\n";
        erg += "PLTE {\n";
        erg += "    (  0,  0,  0)\n";
        erg += "    (255,255,255)\n";
        erg += "    (136,  0,  0)\n";
        erg += "    (170,255,238)\n";
        erg += "    (204, 68,204)\n";
        erg += "    (  0,204, 85)\n";
        erg += "    (  0,  0,170)\n";
        erg += "    (238,238,119)\n";
        erg += "    (221,136, 85)\n";
        erg += "    (102, 68,  0)\n";
        erg += "    (255,119,119)\n";
        erg += "    ( 51, 51, 51)\n";
        erg += "    (119,119,119)\n";
        erg += "    (170,255,102)\n";
        erg += "    (  0,136,255)\n";
        erg += "    (187,187,187)\n";
        erg += "}\n";
        erg += "IMAGE {\n";
        erg += "    pixels base64\n";
        erg += write();
        erg += "}\n";
        return erg;
    }

    private String write_acme()
    {
        int[][] chr = calc_charset();

        String erg = "";
        erg += "!to \""+outname+".prg\", cbm\n\n";
        erg += "!src \"rt_macros.a\"\n";
        erg += "!src \"rti_macros.a\"\n\n";
        erg += "*=$"+Integer.toHexString(descr.PRG).toUpperCase()+"\n\n";
        erg += "            jmp SHOW_IMAGE\n\n";
        erg += "HIDE_IMAGE  +RESET_IRQ\n";
        erg += "            lda #8\n";
        erg += "            sta $D016\n";
        erg += "            lda #32\n";
        erg += "            ldy #11\n";
        erg += "-           sta 1064,y\n";
        erg += "            sta 1104,y\n";
        erg += "            sta 1144,y\n";
        erg += "            sta 1184,y\n";
        erg += "            sta 1224,y\n";
        erg += "            sta 1264,y\n";
        erg += "            sta 1304,y\n";
        erg += "            sta 1344,y\n";
        erg += "            sta 1384,y\n";
        erg += "            sta 1424,y\n";
        erg += "            sta 1464,y\n";
        erg += "            dey\n";
        erg += "            bne -\n";
        erg += "            rts\n\n";
        erg += "SHOW_IMAGE  sei\n";
        erg += "            lda $01\n";
        erg += "            and #%11111110\n";
        erg += "            sta $01\n";
        erg += "            ;+COPY_CHAR_ROM $D000, $2000\n";
        erg += "            +COPY_DATA CHAR_DATA, CHAR_DATA_END, $2400\n";
        erg += "            +COPY_DATA SPRITE_DATA, SPRITE_DATA_END, $2800\n";
        erg += "            +RLINE 70\n";
        erg += "            lda #$"+Integer.toHexString(descr.BGCOL).toUpperCase()+"\n";
        erg += "            sta $D021\n\n";
        erg += "            lda $D018\n";
        erg += "            and #%11110001\n";
        erg += "            ora #%00001000\n";
        erg += "            sta $D018\n\n";
        erg += "            +INIT_IRQ irq, 22\n";
        erg += "            +COPY_CHAR CHAR, "+(1024*descr.SCREEN+40*descr.YPOS+descr.XPOS)+", CHAR_COL, "+(55296+40*descr.YPOS+descr.XPOS)+"\n";
        erg += "            lda $01\n";
        erg += "            ora #%00000001\n";
        erg += "            sta $01\n\n";
        erg += "            rts\n\n";
        erg += "COPY        +COPY\n\n";
        erg += "irq:        +START_IRQ\n\n";
        erg += "            +SYNC 27\n\n";
        erg += "            lda $D016\n";
        erg += "            sta B_CHAR_MC\n";
        erg += "            lda CHAR_MC\n";
        erg += "            sta $D016\n";
        erg += "            lda $D022\n";
        erg += "            sta B_CHAR_COL1\n";
        erg += "            lda CHAR_COL1\n";
        erg += "            sta $D022\n";
        erg += "            lda $D023\n";
        erg += "            sta B_CHAR_COL2\n";
        erg += "            lda CHAR_COL2\n";
        erg += "            sta $D023\n\n";
        erg += "            ldy #$10\n";
        erg += "-           lda $D000,y\n";
        erg += "            sta B_SPRITE_POS,y\n";
        erg += "            lda SPRITE_POS,y\n";
        erg += "            sta $D000,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n\n";
        erg += "            ldy #$09\n";
        erg += "-           lda $D025,y\n";
        erg += "            sta B_SPRITE_COL,y\n";
        erg += "            lda SPRITE_COL,y\n";
        erg += "            sta $D025,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n\n";
        erg += "            ldy #$07\n";
        erg += "-           lda 2040,y\n";
        erg += "            sta B_SPRITEDATA,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n";
        erg += "            clc\n";
        erg += "            lda #$A0\n";
        erg += "            ldy #0\n";
        erg += "-           sta 2040,Y\n";
        erg += "            iny\n";
        erg += "            adc #5\n";
        erg += "            cmp #$C8\n";
        erg += "            bne -\n\n";
        erg += "            lda $D017\n";
        erg += "            sta B_SPRITE_YS\n";
        erg += "            lda #0\n";
        erg += "            sta $D017\n";
        erg += "            lda $D01B\n";
        erg += "            sta B_SPRITE_BG\n";
        erg += "            lda SPRITE_BG\n";
        erg += "            sta $D01B\n";
        erg += "            lda $D01C\n";
        erg += "            sta B_SPRITE_MC\n";
        erg += "            lda SPRITE_MC\n";
        erg += "            sta $D01C\n";
        erg += "            lda $D01D\n";
        erg += "            sta B_SPRITE_XS\n";
        erg += "            lda SPRITE_XS\n";
        erg += "            sta $D01D\n";
        erg += "            lda $D015\n";
        erg += "            sta B_SPRITE_ON\n";
        erg += "            lda #255\n";
        erg += "            sta $D015\n\n";
        erg += "            +RLINE 52\n\n";
        erg += "            lda #42+21\n";
        erg += "            sta $D001\n";
        erg += "            sta $D003\n";
        erg += "            lda #43+21\n";
        erg += "            sta $D005\n";
        erg += "            sta $D007\n";
        erg += "            lda #45+21\n";
        erg += "            sta $D009\n";
        erg += "            sta $D00B\n";
        erg += "            lda #48+21\n";
        erg += "            sta $D00D\n";
        erg += "            sta $D00F\n\n";
        erg += "            +RLINE 58\n";
        erg += "            +WAIT 41\n\n"; // Nach dem WAIT: 60/11 ggfs. nachsynchronisieren!

        erg += Switch.calc_schedule(switches)+"\n";

        erg += "            +RLINE 147\n\n";
        erg += "            lda B_CHAR_MC\n";
        erg += "            sta $D016\n";
        erg += "            lda B_CHAR_COL1\n";
        erg += "            sta $D022\n";
        erg += "            lda B_CHAR_COL2\n";
        erg += "            sta $D023\n\n";
        erg += "            ldy #$10\n";
        erg += "-           lda B_SPRITE_POS,y\n";
        erg += "            sta $D000,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n\n";
        erg += "            ldy #$09\n";
        erg += "-           lda B_SPRITE_COL,y\n";
        erg += "            sta $D025,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n\n";
        erg += "            ldy #$07\n";
        erg += "-           lda B_SPRITEDATA,y\n";
        erg += "            sta 2040,y\n";
        erg += "            dey\n";
        erg += "            bpl -\n\n";
        erg += "            lda B_SPRITE_YS\n";
        erg += "            sta $D017\n";
        erg += "            lda B_SPRITE_BG\n";
        erg += "            sta $D01B\n";
        erg += "            lda B_SPRITE_MC\n";
        erg += "            sta $D01C\n";
        erg += "            lda B_SPRITE_XS\n";
        erg += "            sta $D01D\n";
        erg += "            lda B_SPRITE_ON\n";
        erg += "            sta $D015\n\n";
        erg += "            +END_IRQ\n\n";

        erg += "CHAR_MC     !by "+(data.chars.mode==1?24:8)+"\n";
        erg += "CHAR_COL1   !by "+data.chars.col1+"\n";
        erg += "CHAR_COL2   !by "+data.chars.col2+"\n";
        erg += "SPRITE_POS  !by ";
        int bit9 = 0;
        for (int s=0;s<8;s++)
        {
            int x = 8*descr.XPOS+data.sprites.sprite[s].x+24;
            int y = 58+OFFSET[s];
            erg += (x & 255)+","+y+",";
            if (x>255) bit9 += 1<<s;
        }
        erg += bit9+"\n";
        erg += "SPRITE_COL  !by "+data.sprites.col1+","+data.sprites.col2;
        for (int s=0;s<8;s++)
            erg += ","+data.sprites.sprite[s].col;
        erg += "\n";
        erg += "SPRITE_BG   !by "+data.sprites.get_bg()+"\n";
        erg += "SPRITE_MC   !by "+data.sprites.get_mc()+"\n";
        erg += "SPRITE_XS   !by "+data.sprites.get_xs()+"\n\n";

        erg += "B_CHAR_MC     !by 0\n";
        erg += "B_CHAR_COL1   !by 0\n";
        erg += "B_CHAR_COL2   !by 0\n";
        erg += "B_SPRITE_POS  !by 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n";
        erg += "B_SPRITE_COL  !by 0,0,0,0,0,0,0,0,0,0\n";
        erg += "B_SPRITE_BG   !by 0\n";
        erg += "B_SPRITE_MC   !by 0\n";
        erg += "B_SPRITE_XS   !by 0\n";
        erg += "B_SPRITE_YS   !by 0\n";
        erg += "B_SPRITE_ON   !by 0\n";
        erg += "B_SPRITEDATA\n\n";

        int az = charheight()*charwidth()*2+8*(charset.charset_end-charset.charset_start)+40*64;

        erg += "*=$"+Integer.toHexString(49152-az).toUpperCase()+"\n\n";

        erg += "CHAR        !by ";
        for (int j=0;j<charheight();j++)
            for (int i=0;i<charwidth();i++)
                erg += chr[i][j]+",";
        erg = erg.substring(0,erg.length()-1);
        erg += "\n";

        erg += "CHAR_COL    !by ";
        for (int j=0;j<charheight();j++)
            for (int i=0;i<charwidth();i++)
                erg += data.chars.chr[i][j].c+",";
        erg = erg.substring(0,erg.length()-1);
        erg += "\n";

        erg += "CHAR_DATA   !by ";
        for (int c=charset.charset_start;c<charset.charset_end;c++)
        {
            for (int j=0;j<8;j++)
            {
                int val = 0;
                for (int i=0;i<8;i++)
                {
                    val *= 2;
                    val += charset.charset[c][i][j];
                }
                erg += val+",";
            }
        }
        erg = erg.substring(0,erg.length()-1);
        erg += "\nCHAR_DATA_END\n";

        erg += "SPRITE_DATA !by ";
        for (int s=0;s<8;s++)
        {
            if (s!=0) erg += "            !by ";
            for (int i=0;i<5;i++)
                erg += data.sprites.sprite[s].get_data(21*i+OFFSET[s])+",0"+(i<4?",":"");
            erg += "\n";
        }
        erg += "SPRITE_DATA_END\n";

        return erg;
    }

    private String status()
    {
        int corr = correct();
        int max = 64*charwidth()*charheight();
        return corr+"/"+max+" ("+(Math.round(10000.0/max*corr)/100.0)+"%)";
    }

    private int correct()
    {
        int corr = 0;
        for (int j=0;j<height();j++)
            for (int i=0;i<width();i++)
                if (data.get_pixel(i,j,descr.BGCOL)==goal[i][j])
                    corr++;
        return corr;
    }

    private int correct(int xs, int ys, int xe, int ye)
    {
        int corr = 0;
        for (int j=ys;j<ye;j++)
            for (int i=xs;i<xe;i++)
                if (data.get_pixel(i,j,descr.BGCOL)==goal[i][j])
                    corr++;
        return corr;
    }

    private int correct_save(int xs, int ys, int xe, int ye)
    {
        xs = Math.max(0,xs);
        ys = Math.max(0,ys);
        xe = Math.min(width(),xe);
        ye = Math.min(height(),ye);

        int corr = 0;
        for (int j=ys;j<ye;j++)
            for (int i=xs;i<xe;i++)
                if (data.get_pixel(i,j,descr.BGCOL)==goal[i][j])
                    corr++;
        return corr;
    }

    private String sprite_status()
    {
        StringBuffer b = new StringBuffer();
        for (int i=0;i<8;i++)
            b.append(i+": "+data.sprites.sprite[i].status()+"\n");
        return b.toString();
    }

    //////////////////////////////////////////////////////////////////

    private void approx_with_sprites_and_chars()
    {
        boolean optimum = false;

        PROGRESS--;
        boolean[] colors = count_colors();
        count_hires_spriteareas();
        int best = -1;
        Sprites bestsprites = new Sprites();
        Chars bestchars = new Chars(charwidth(),charheight());
        for (int c1=0;c1<15;c1++) if (colors[c1])
            for (int c2=c1+1;c2<16;c2++) if (colors[c2])
            {
                if (optimum) continue;
                if (PROGRESS>=0)
                    System.err.print("SC "+c1+" "+c2+"  \r");
                if (PROGRESS>0)
                    System.err.println();
                count_multi_spriteareas();
                data.sprites = new Sprites();
                data.chars = new Chars(charwidth(),charheight());
                data.sprites.col1 = c1;
                data.sprites.col2 = c2;

                int last = -1;
                while (true)
                {
                    add_best_sprites();
                    add_best_chars();
                    if (correct()==last)
                        try_all_pixels();
                    int corr = correct();
                    if (corr==last) break;
                    if (corr<last)
                    {
                        System.err.println("Somethings going wrong... "+corr+"<"+last);
                        save_dump();
                    }
                    last = corr;
                }

                int corr = correct();
                if (PROGRESS>=0)
                    System.err.print("SC "+c1+" "+c2+" "+status());
                if (corr>best)
                {
                    best = corr;
                    bestsprites = new Sprites(data.sprites);
                    bestchars = new Chars(data.chars);
                    save_dump();
                    System.err.println(" <== best");
                    if (corr==64*charwidth()*charheight())
                        optimum = true;
                }
                else
                    System.err.println();
            }

        data.sprites = bestsprites;
        data.chars = bestchars;
        PROGRESS++;
    }

    private void test()
    {
        System.err.println("No test available.");
        System.exit(-1);
    }

    //////////////////////////////////////////////////////////////////

    private void add_best_chars()
    {
        PROGRESS--;
        if (PROGRESS>=0)
            System.err.print("HR\r");
        data.chars.mode = 0;
        calc_best_chars();
        long best = 0;
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
            {
                int hlp = data.chars.get_pixel(i,j);
                if (hlp==-1) hlp = descr.BGCOL;
                if (hlp==goal[i][j])
                    best++;
            }
        best += 100000L*correct();
        Data bestdata = new Data(data);
        if (PROGRESS>=0)
            System.err.println("HR "+status());

        data.chars.mode = 1;
        for (int c1=0;c1<16;c1++)
            for (int c2=0;c2<16;c2++)
                {
                    if (PROGRESS>=0)
                        System.err.print("MC "+c1+" "+c2+"  \r");
                    data.chars.col1 = c1;
                    data.chars.col2 = c2;
                    calc_best_chars();

                    long corr = 0;
                    for (int i=0;i<width();i++)
                        for (int j=0;j<height();j++)
                        {
                            int hlp = data.chars.get_pixel(i,j);
                            if (hlp==-1) hlp = descr.BGCOL;
                            if (hlp==goal[i][j])
                                corr++;
                        }
                    corr += 100000L*correct();
                    if (corr>best)
                    {
                        best = corr;
                        bestdata = new Data(data);
                        if (PROGRESS>=0)
                            System.err.println("MC "+c1+" "+c2+" "+status());
                    }
                }

        data = bestdata;
        PROGRESS++;
    }

    private void calc_best_chars()
    {
        int[][] other = new int[width()][height()];
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
                other[i][j] = data.get_other(i,j);

        for (int i=0;i<charwidth();i++)
            for (int j=0;j<charheight();j++)
                calc_best_char(i,j,other);
    }

    private void calc_best_char(int x, int y, int[][] other)
    {
        int best = -1;
        Data bestdata = null;

        for (int col=0;col<16;col++)
        {
            data.chars.chr[x][y].c = col;
            if (col<8 || data.chars.mode==0)
                calc_best_hires_char(x,y,col,other);
            else
                calc_best_mc_char(x,y,col,other);

            int corr = 0;
            for (int i=0;i<8;i++)
                for (int j=0;j<8;j++)
                {
                    int hlp = data.chars.get_pixel(8*x+i,8*y+j);
                    if (hlp==-1) hlp = descr.BGCOL;
                    if (hlp==goal[8*x+i][8*y+j])
                        corr++;
                }
            corr += 100*correct(8*x,8*y,8*x+8,8*y+8);
            if (corr>best)
            {
                best = corr;
                bestdata = new Data(data);
            }
        }

        data = bestdata;
    }

    private void calc_best_hires_char(int x, int y, int col, int[][] other)
    {
        for (int i=0;i<8;i++)
            for (int j=0;j<8;j++)
                data.chars.chr[x][y].p[i][j] = col==goal[8*x+i][8*y+j]?1:0;
    }

    private void calc_best_mc_char(int x, int y, int col, int[][] other)
    {
        for (int i=0;i<8;i+=2)
            for (int j=0;j<8;j++)
            {
                int[] p = new int[4];

                for (int k=0;k<=1;k++)
                {
                    if (other[8*x+i+k][8*y+j]==-2)
                    {
                        if (col-8==goal[8*x+i+k][8*y+j]) p[3]++;
                        if (data.chars.col2==goal[8*x+i+k][8*y+j]) p[2]++;
                        if (data.chars.col1==goal[8*x+i+k][8*y+j]) p[1]++;
                        if (descr.BGCOL==goal[8*x+i+k][8*y+j]) p[0]++;
                    }
                    if (other[8*x+i+k][8*y+j]>=0 && other[8*x+i+k][8*y+j]!=goal[8*x+i+k][8*y+j])
                    {
                        if (col-8==goal[8*x+i+k][8*y+j]) p[3]++;
                        if (data.chars.col2==goal[8*x+i+k][8*y+j]) p[2]++;
                    }
                }

                if (p[3]>p[2] && p[3]>p[1] && p[3]>p[0])
                {
                    data.chars.chr[x][y].p[i][j] = 1;
                    data.chars.chr[x][y].p[i+1][j] = 1;
                }
                else if (p[2]>p[1] && p[2]>p[0])
                {
                    data.chars.chr[x][y].p[i][j] = 1;
                    data.chars.chr[x][y].p[i+1][j] = 0;
                }
                else if (p[1]>p[0])
                {
                    data.chars.chr[x][y].p[i][j] = 0;
                    data.chars.chr[x][y].p[i+1][j] = 1;
                }
                else if (p[0]>0 || other[8*x+i][8*y+j]>=0 || other[8*x+i+1][8*y+j]>=0)
                {
                    data.chars.chr[x][y].p[i][j] = 0;
                    data.chars.chr[x][y].p[i+1][j] = 0;
                }
                else if (col-8==goal[8*x+i][8*y+j] || col-8==goal[8*x+i+1][8*y+j])
                {
                    data.chars.chr[x][y].p[i][j] = 1;
                    data.chars.chr[x][y].p[i+1][j] = 1;
                }
                else if (data.chars.col2==goal[8*x+i][8*y+j] || data.chars.col2==goal[8*x+i+1][8*y+j])
                {
                    data.chars.chr[x][y].p[i][j] = 1;
                    data.chars.chr[x][y].p[i+1][j] = 0;
                }
                else if (data.chars.col1==goal[8*x+i][8*y+j] || data.chars.col1==goal[8*x+i+1][8*y+j])
                {
                    data.chars.chr[x][y].p[i][j] = 0;
                    data.chars.chr[x][y].p[i+1][j] = 1;
                }
                else
                {
                    data.chars.chr[x][y].p[i][j] = 0;
                    data.chars.chr[x][y].p[i+1][j] = 0;
                }
            }
    }

    //////////////////////////////////////////////////////////////////

    private void add_best_sprites()
    {
        PROGRESS--;
        for (int s=7;s>=0;s--)
        {
            if (PROGRESS>=0)
                System.err.print("S"+s+"  \r");

            int[][][] plane = calc_plane(s);

            int orig = 10000*correct();
            Sprite os = data.sprites.sprite[s];

            Sprite s1 = calc_best_hires_sprite(s, plane);
            Sprite s2 = calc_best_mc_sprite(s, plane);

            data.sprites.sprite[s] = s1;
            int corr1 = 0;
            for (int i=0;i<width();i++)
                for (int j=0;j<height();j++)
                {
                    int hlp = data.sprites.sprite[s].get_pixel(i,j,data.sprites.col1,data.sprites.col2);
                    if (hlp==-1) hlp = descr.BGCOL;
                    if (hlp==goal[i][j])
                        corr1++;
                }
            corr1 += 10000*correct();

            data.sprites.sprite[s] = s2;
            int corr2 = 0;
            for (int i=0;i<width();i++)
                for (int j=0;j<height();j++)
                {
                    int hlp = data.sprites.sprite[s].get_pixel(i,j,data.sprites.col1,data.sprites.col2);
                    if (hlp==-1) hlp = descr.BGCOL;
                    if (hlp==goal[i][j])
                        corr2++;
                }
            corr2 += 10000*correct();

            if (corr1>orig && corr1>corr2)
                data.sprites.sprite[s] = s1;
            else if (corr2<=orig)
            {
                System.err.println("error in add_best_sprites: "+s+" "+corr1+" "+corr2+" "+orig);
                System.err.println(s2.status());
                for (int j=0;j<8*descr.HEIGHT;j++)
                {
                    for (int i=0;i<24;i++)
                        System.err.print(s2.d[i][j]);
                    System.err.println();
                }

                data.sprites.sprite[s] = os; // War keine Verbesserung...
            }

            if (PROGRESS>=0)
            {
                Sprite h = data.sprites.sprite[s];
                System.err.println("S"+s+" "+h.col+" @"+h.x+","+h.y
                                   +" "+h.xs+" "+h.ys+" "+(h.bg==0?"fg":"bg")
                                   +" "+(h.mode==0?"hr":"mc")+" "+status());
            }
        }
        PROGRESS++;
    }

    private Sprite calc_best_hires_sprite(int n, int[][][] plane)
    {
        int best = -1;
        int besti = 0;
        int bestc = 0;
        int bestxs = 0;
        int bestbg = 0;

        for (int bg=0;bg<=1;bg++)
            for (int xs=0;xs<=1;xs++)
                for (int col=0;col<16;col++)
                    for (int xp=hr_sa[col][xs==1?2:0];xp<hr_sa[col][xs==1?3:1];xp++)
                    {
                        int val = 0;
                        for (int i=0;i<24;i++)
                            for (int j=0;j<8*descr.HEIGHT;j++)
                            {
                                int xx = xp+(xs+1)*i;
                                int yy = j;
                                int loc = 0;
                                for (int ii=0;ii<=xs;ii++)
                                {
                                    int x = xx+ii;
                                    int y = yy;
                                    int g = goal[x][y];
                                    if (col==g) loc++;
                                    if (plane[1][x][y]==1) continue;
                                    if (bg==0)
                                    {
                                        if (plane[0][x][y]==g)
                                        {
                                            if (col!=g) loc-=10000;
                                        }
                                        else
                                        {
                                            if (col==g) loc+=10000;
                                        }
                                    }
                                    else
                                    {
                                        int c = plane[2][x][y]==-1?col:plane[2][x][y];
                                        if (plane[0][x][y]==g)
                                        {
                                            if (c!=g) loc-=10000;
                                        }
                                        else
                                        {
                                            if (c==g) loc+=10000;
                                        }
                                    }
                                }
                                if (loc>0) val+=loc;
                            }
                        if (val>best)
                        {
                            best = val;
                            besti = xp;
                            bestc = col;
                            bestxs = xs;
                            bestbg = bg;
                        }
                    }

        Sprite s = new Sprite();
        s.mode = 0;
        s.bg = bestbg;
        s.col = bestc;
        s.xs = bestxs;
        s.ys = 0;
        s.x = besti;
        s.y = 0;
        if (best==-1) return s;

        for (int i=0;i<24;i++)
            for (int j=0;j<8*descr.HEIGHT;j++)
            {
                int loc = 0;
                for (int ii=0;ii<=bestxs;ii++)
                {
                    int x = besti+i+bestxs*i+ii;
                    int y = j;
                    if (plane[1][x][y]==1) continue;
                    if (bestbg==0)
                    {
                        if (plane[0][x][y]==goal[x][y] && bestc!=goal[x][y]) loc--;
                        if (plane[0][x][y]!=goal[x][y] && bestc==goal[x][y]) loc++;
                    }
                    else
                    {
                        int c = plane[2][x][y]==-1?bestc:plane[2][x][y];
                        if (plane[0][x][y]==goal[x][y] && c!=goal[x][y]) loc--;
                        if (plane[0][x][y]!=goal[x][y] && c==goal[x][y]) loc++;
                    }
                }
                if (loc>0)
                    s.d[i][j] = 1;
                else if (loc<0)
                    s.d[i][j] = 0;
                else
                {
                    for (int ii=0;ii<=bestxs;ii++)
                    {
                        int x = besti+i+bestxs*i+ii;
                        int y = j;
                        if (goal[x][y]==bestc) loc++;
                    }
                    s.d[i][j] = loc>0?1:0;
                }
            }

        return s;
    }

    private Sprite calc_best_mc_sprite(int n, int[][][] plane)
    {
        int best = -1;
        int besti = 0;
        int bestc = 0;
        int bestxs = 0;
        int bestbg = 0;

        for (int bg=0;bg<=1;bg++)
            for (int xs=0;xs<=1;xs++)
                for (int col=0;col<16;col++) if (col!=data.sprites.col1 && col!=data.sprites.col2)
                    for (int xp=mc_sa[col][xs==1?2:0];xp<mc_sa[col][xs==1?3:1];xp++)
                    {
                        int val = 0;
                        for (int i=0;i<24;i+=2)
                            for (int j=0;j<8*descr.HEIGHT;j++)
                            {
                                int xx = xp+(xs+1)*i;
                                int yy = j;
                                int loc0 = 0;
                                int loc1 = 0;
                                int loc2 = 0;
                                for (int ii=0;ii<=xs;ii++)
                                    for (int k=0;k<=1;k++)
                                    {
                                        int x = xx+(xs+1)*k+ii;
                                        int y = yy;
                                        int g = goal[x][y];
                                        if (plane[1][x][y]==1) continue;
                                        if (bg==0)
                                        {
                                            if (plane[0][x][y]==g)
                                            {
                                                if (col!=g) loc0-=10000;
                                                if (data.sprites.col1!=g) loc1-=10000;
                                                if (data.sprites.col2!=g) loc2-=10000;
                                            }
                                            else
                                            {
                                                if (col==g) loc0+=10000;
                                                if (data.sprites.col1==g) loc1+=10000;
                                                if (data.sprites.col2==g) loc2+=10000;
                                            }
                                        }
                                        else
                                        {
                                            int c1 = col;
                                            int c2 = data.sprites.col1;
                                            int c3 = data.sprites.col2;
                                            if (plane[2][x][y]!=-1)
                                                c1 = c2 = c3 = plane[2][x][y];
                                            if (plane[0][x][y]==g)
                                            {
                                                if (c1!=g) loc0-=10000;
                                                if (c2!=g) loc1-=10000;
                                                if (c3!=g) loc2-=10000;
                                            }
                                            else
                                            {
                                                if (c1==g) loc0+=10000;
                                                if (c2==g) loc1+=10000;
                                                if (c3==g) loc2+=10000;
                                            }
                                        }
                                    }
                                if (loc0>0 && loc0>loc1 && loc0>loc2)
                                    val += loc0;
                                else if (loc1>0 && loc1>loc2)
                                    val += loc1;
                                else if (loc2>0)
                                    val += loc2;
                            }
                        if (val>best)
                        {
                            best = val;
                            besti = xp;
                            bestc = col;
                            bestxs = xs;
                            bestbg = bg;
                        }
                    }

        Sprite s = new Sprite();
        s.mode = 1;
        s.bg = bestbg;
        s.col = bestc;
        s.xs = bestxs;
        s.ys = 0;
        s.x = besti;
        s.y = 0;
        if (best==-1) return s;

        for (int i=0;i<24;i+=2)
            for (int j=0;j<8*descr.HEIGHT;j++)
            {
                int[] loc = new int[3];
                for (int ii=0;ii<=bestxs;ii++)
                    for (int k=0;k<=1;k++)
                    {
                        int x = besti+(bestxs+1)*(i+k)+ii;
                        int y = j;
                        if (plane[1][x][y]==1) continue;
                        if (bestbg==0)
                        {
                            if (plane[0][x][y]==goal[x][y] && bestc!=goal[x][y]) loc[0]-=10;
                            if (plane[0][x][y]!=goal[x][y] && bestc==goal[x][y]) loc[0]+=10;
                            if (plane[0][x][y]==goal[x][y] && data.sprites.col1!=goal[x][y]) loc[1]-=10;
                            if (plane[0][x][y]!=goal[x][y] && data.sprites.col1==goal[x][y]) loc[1]+=10;
                            if (plane[0][x][y]==goal[x][y] && data.sprites.col2!=goal[x][y]) loc[2]-=10;
                            if (plane[0][x][y]!=goal[x][y] && data.sprites.col2==goal[x][y]) loc[2]+=10;
                        }
                        else
                        {
                            int c = plane[2][x][y]==-1?bestc:plane[2][x][y];
                            if (plane[0][x][y]==goal[x][y] && c!=goal[x][y]) loc[0]-=10;
                            if (plane[0][x][y]!=goal[x][y] && c==goal[x][y]) loc[0]+=10;
                            c = plane[2][x][y]==-1?data.sprites.col1:plane[2][x][y];
                            if (plane[0][x][y]==goal[x][y] && c!=goal[x][y]) loc[1]-=10;
                            if (plane[0][x][y]!=goal[x][y] && c==goal[x][y]) loc[1]+=10;
                            c = plane[2][x][y]==-1?data.sprites.col2:plane[2][x][y];
                            if (plane[0][x][y]==goal[x][y] && c!=goal[x][y]) loc[2]-=10;
                            if (plane[0][x][y]!=goal[x][y] && c==goal[x][y]) loc[2]+=10;
                        }
                    }
                if (loc[0]>0 && loc[0]>loc[1] && loc[0]>loc[2])
                {
                    s.d[i][j] = 1;
                    s.d[i+1][j] = 0;
                }
                else if (loc[1]>0 && loc[1]>loc[2])
                {
                    s.d[i][j] = 0;
                    s.d[i+1][j] = 1;
                }
                else if (loc[2]>0)
                {
                    s.d[i][j] = 1;
                    s.d[i+1][j] = 1;
                }
                else if (loc[0]<0 && loc[1]<0 && loc[2]<0)
                {
                    s.d[i][j] = 0;
                    s.d[i+1][j] = 0;
                }
                else
                {
                    for (int ii=0;ii<=bestxs;ii++)
                        for (int k=0;k<=1;k++)
                        {
                            int x = besti+(bestxs+1)*(i+k)+ii;
                            int y = j;
                            if (bestc==goal[x][y]) loc[0]++;
                            if (data.sprites.col1==goal[x][y]) loc[1]++;
                            if (data.sprites.col2==goal[x][y]) loc[2]++;
                        }
                    if (loc[0]>loc[1] && loc[0]>loc[2])
                    {
                        s.d[i][j] = 1;
                        s.d[i+1][j] = 0;
                    }
                    else if (loc[1]>loc[2])
                    {
                        s.d[i][j] = 0;
                        s.d[i+1][j] = 1;
                    }
                    else if (loc[2]>0)
                    {
                        s.d[i][j] = 1;
                        s.d[i+1][j] = 1;
                    }
                    else
                    {
                        s.d[i][j] = 0;
                        s.d[i+1][j] = 0;
                    }
                }
            }

        return s;
    }

    private int[][][] calc_plane(int nr)
    {
        // 0 = bg; 1 = fg; 2 = bg vor sprite
        int[][][] erg = new int[3][width()][height()];

        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
            {
                erg[0][i][j] = data.get_background(nr,i,j,descr.BGCOL);
                erg[2][i][j] = data.get_trans_background(i,j);
            }

        for (int i=0;i<nr;i++)
            data.sprites.sprite[i].add_to_plane(erg[1]);

        return erg;
    }

    //////////////////////////////////////////////////////////////////

    private int search_best;
    private Data search_data;

    private void try_all_pixels()
    {
        PROGRESS--;
        int best = correct();
        Data bestdata = new Data(data);
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
                if (data.get_pixel(i,j,descr.BGCOL)!=goal[i][j])
                {
                    if (PROGRESS>=0)
                        System.err.print("P"+i+","+j+"  \r");

                    improve_pixel(i,j);

                    int corr = correct();
                    if (corr>best)
                    {
                        best = corr;
                        bestdata = new Data(data);
                        if (PROGRESS>=0)
                            System.err.println("P"+i+","+j+" "+status());
                    }
                }

        data = bestdata;
        PROGRESS++;
    }

    private void improve_pixel(int x, int y)
    {
        search_best = correct_save(x-3,y-1,x+4,y+2);
        search_data = new Data(data);
        search_pixel_at(0,x,y);
        data = search_data;
    }

    private void search_pixel_at(int n, int x, int y)
    {
        if (n==9)
        {
            int corr = correct_save(x-3,y-1,x+4,y+2);
            if (corr>search_best)
            {
                search_best = corr;
                search_data = new Data(data);
            }
            return;
        }

        if (n==8)
        {
            int xc = x/8;
            int yc = y/8;
            int xp = x%8;
            int yp = y%8;

            if (data.chars.mode==0)
            {
                for (int p=0;p<=1;p++)
                {
                    data.chars.chr[xc][yc].p[xp][yp] = p;
                    search_pixel_at(9,x,y);
                }
            }
            else
            {
                if (xp%2==1) xp--;
                for (int p1=0;p1<=1;p1++)
                {
                    data.chars.chr[xc][yc].p[xp][yp] = p1;
                    for (int p2=0;p2<=1;p2++)
                    {
                        data.chars.chr[xc][yc].p[xp+1][yp] = p2;
                        search_pixel_at(9,x,y);
                    }
                }
            }
            return;
        }

        int xs = x-data.sprites.sprite[n].x;
        int ys = y-data.sprites.sprite[n].y;
        if (xs<0 || ys<0 || xs>=((data.sprites.sprite[n].xs==1)?48:24) || ys>=((data.sprites.sprite[n].ys==1)?42:8*descr.HEIGHT))
        {
            search_pixel_at(n+1,x,y);
            return;
        }

        if (data.sprites.sprite[n].xs==1) xs/=2;
        if (data.sprites.sprite[n].ys==1) ys/=2;

        if (data.sprites.sprite[n].mode==0)
        {
            for (int p=0;p<=1;p++)
            {
                data.sprites.sprite[n].d[xs][ys] = p;
                search_pixel_at(n+1,x,y);
            }
        }
        else
        {
            if (xs%2==1) xs--;
            for (int p1=0;p1<=1;p1++)
            {
                data.sprites.sprite[n].d[xs][ys] = p1;
                for (int p2=0;p2<=1;p2++)
                {
                    data.sprites.sprite[n].d[xs+1][ys] = p2;
                    search_pixel_at(n+1,x,y);
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////

    private int[][] calc_charset()
    {
        if (charset.charset==null)
            charset.charset = new int[256][8][8];

        int[][] chr = new int[charwidth()][charheight()];

        int nextchar = charset.charset_start = descr.CHARAVAIL[0];
        for (int i=0;i<charwidth();i++)
a:          for (int j=0;j<charheight();j++)
            {
                for (int k=descr.CHARAVAIL[0];k<nextchar;k++)
                {
                    boolean found = true;
b:                  for (int ii=0;ii<8;ii++)
                        for (int jj=0;jj<8;jj++)
                            if (charset.charset[k][ii][jj]!=data.chars.chr[i][j].p[ii][jj])
                            {
                                found = false;
                                break b;
                            }

                    if (found)
                    {
                        chr[i][j] = k;
                        continue a;
                    }
                }

                if (nextchar>descr.CHARAVAIL[1])
                {
                    System.err.println("Too many characters. Using random char.");
                    for (int ii=0;ii<8;ii++)
                        for (int jj=0;jj<8;jj++)
                            data.chars.chr[i][j].p[ii][jj] = charset.charset[nextchar-1][ii][jj];
                    chr[i][j] = nextchar-1;
                    continue;
                }

                for (int ii=0;ii<8;ii++)
                    for (int jj=0;jj<8;jj++)
                        charset.charset[nextchar][ii][jj] = data.chars.chr[i][j].p[ii][jj];
                chr[i][j] = nextchar++;
            }

        charset.charset_end = nextchar;
        return chr;
    }

    //////////////////////////////////////////////////////////////////

    private int width()
    {
        return goal.length;
    }

    private int height()
    {
        return goal[0].length;
    }

    private int charwidth()
    {
        return goal.length/8;
    }

    private int charheight()
    {
        return goal[0].length/8;
    }

    //////////////////////////////////////////////////////////////////

    private boolean[] count_colors()
    {
        if (USE_BRUTE_FORCE>1)
        {
            boolean[] erg = new boolean[16];
            for (int i=0;i<16;i++)
                erg[i] = true;
            return erg;
        }

        int[] col = new int[16];
        for (int i=0;i<charwidth();i++)
            for (int j=0;j<charheight();j++)
            {
                boolean[] da = new boolean[16];
                for (int ii=0;ii<8;ii++)
                    for (int jj=0;jj<8;jj++)
                        da[goal[8*i+ii][8*j+jj]] = true;

                for (int k=0;k<16;k++)
                    if (da[k]) col[k]++;
            }
        if (USE_BRUTE_FORCE==0)
            col[descr.BGCOL] = 0;

        int cnt = 0;
        int max = 0;
        for (int i=0;i<16;i++)
        {
            if (col[i]>max) max = col[i];
            if (col[i]>0) cnt++;
        }

        boolean[] erg = new boolean[16];
        if (cnt<3 || USE_BRUTE_FORCE>0)
        {
            for (int i=0;i<16;i++)
                erg[i] = col[i]>0;
            if (cnt<2) erg[0] = erg[1] = true; // Einfarbiges Bild...
        }
        else
        {
            while (true)
            {
                for (int i=0;i<16;i++)
                    erg[i] = (i<8 && col[i]>=4*max/5) || (i>=8 && col[i]>=3*max/5);

                cnt = 0;
                for (int i=0;i<16;i++)
                    if (erg[i])
                        cnt++;

                if (cnt>2) break;
                max = 4*max/5-1;
            }
        }

        if (PROGRESS>=0)
        {
            System.err.print("Trying colors: ");
            for (int i=0;i<16;i++)
                if (erg[i])
                    System.err.print(i+", ");
            System.err.println();
        }

        return erg;
    }

    private void count_hires_spriteareas()
    {
        if (USE_BRUTE_FORCE>1)
        {
            hr_sa = new int[16][4];
            for (int c=0;c<16;c++)
            {
                hr_sa[c][0] = 0;
                hr_sa[c][1] = width()-24;
                hr_sa[c][2] = 0;
                hr_sa[c][3] = width()-48;
            }
            return;
        }

        int[] minx = new int[16];
        int[] maxx = new int[16];
        for (int i=0;i<16;i++)
        {
            minx[i] = width()*height()+1;
            maxx[i] = -1;
        }
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
            {
                if (i<minx[goal[i][j]]) minx[goal[i][j]] = i;
                if (i>maxx[goal[i][j]]) maxx[goal[i][j]] = i;
            }

        hr_sa = new int[16][4];
        for (int c=0;c<16;c++)
        {
            hr_sa[c][0] = minx[c];
            hr_sa[c][1] = maxx[c]-23;
            if (hr_sa[c][1]<=hr_sa[c][0]) hr_sa[c][1] = hr_sa[c][0]+1;
            if (hr_sa[c][1]>=width()-24)
            {
                hr_sa[c][0] -= hr_sa[c][1]-(width()-24);
                if (hr_sa[c][0]<0) hr_sa[c][0]=0;
                hr_sa[c][1] = width()-24;
            }

            hr_sa[c][2] = minx[c]-1;
            hr_sa[c][3] = maxx[c]-47;
            if (hr_sa[c][2]<0) hr_sa[c][2] = 0;
            if (hr_sa[c][3]<=hr_sa[c][2]) hr_sa[c][3] = hr_sa[c][2]+1;
            if (hr_sa[c][3]>=width()-48)
            {
                hr_sa[c][2] -= hr_sa[c][3]-(width()-48);
                if (hr_sa[c][2]<0) hr_sa[c][2]=0;
                hr_sa[c][3] = width()-48;
            }
        }
    }

    private void count_multi_spriteareas()
    {
        if (USE_BRUTE_FORCE>1)
        {
            mc_sa = new int[16][4];
            for (int c=0;c<16;c++)
            {
                mc_sa[c][0] = 0;
                mc_sa[c][1] = width()-24;
                mc_sa[c][2] = 0;
                mc_sa[c][3] = width()-48;
            }
            return;
        }

        int[] minx = new int[16];
        int[] maxx = new int[16];
        int minxx = width()*height()+1;
        int maxxx = -1;
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
                if (goal[i][j]==data.sprites.col1 || goal[i][j]==data.sprites.col2)
                {
                    if (i<minxx) minxx = i;
                    if (i>maxxx) maxxx = i;
                }
        for (int i=0;i<16;i++)
        {
            minx[i] = minxx;
            maxx[i] = maxxx;
        }
        for (int i=0;i<width();i++)
            for (int j=0;j<height();j++)
            {
                if (i<minx[goal[i][j]]) minx[goal[i][j]] = i;
                if (i>maxx[goal[i][j]]) maxx[goal[i][j]] = i;
            }

        mc_sa = new int[16][4];
        for (int c=0;c<16;c++)
        {
            mc_sa[c][0] = minx[c]-1;
            mc_sa[c][1] = maxx[c]-22;
            if (mc_sa[c][0]<0) mc_sa[c][0]=0;
            if (mc_sa[c][1]<=mc_sa[c][0]) mc_sa[c][1] = mc_sa[c][0]+1;
            if (mc_sa[c][1]>=width()-24)
            {
                mc_sa[c][0] -= mc_sa[c][1]-(width()-24);
                if (mc_sa[c][0]<0) mc_sa[c][0]=0;
                mc_sa[c][1] = width()-24;
            }

            mc_sa[c][2] = minx[c]-3;
            mc_sa[c][3] = maxx[c]-45;
            if (mc_sa[c][2]<0) mc_sa[c][2] = 0;
            if (mc_sa[c][3]<=mc_sa[c][2]) mc_sa[c][3] = mc_sa[c][2]+1;
            if (mc_sa[c][3]>=width()-48)
            {
                mc_sa[c][2] -= mc_sa[c][3]-(width()-48);
                if (mc_sa[c][2]<0) mc_sa[c][2]=0;
                mc_sa[c][3] = width()-48;
            }
        }
    }

    //////////////////////////////////////////////////////////////////

    private void save_dump()
    {
        /*
        String filename = "dump."+System.currentTimeMillis();
        System.err.print("Dumping to "+filename);
        try {
            PrintWriter p = new PrintWriter(new FileWriter(filename));
            p.print("STATUS: "+status()+"\n");
            p.print(data.dump());
            p.close();
            System.err.println(" done.");
        } catch (IOException e) { e.printStackTrace(); }
         */
    }

    private void read_dump(String filename)
    {
        data.read_dump(filename);
    }
}

class Description
{
    public int XPOS;
    public int YPOS;
    public int BGCOL;
    public int[] CHARAVAIL;
    public int SCREEN;
    public int PRG;

    private int WIDTH;
    public int HEIGHT;
    private int[][] IMAGE;

    public Description(String filename)
    {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            String[] f = lines.toArray(new String[lines.size()]);

            int p = 0;
            while (p<f.length)
            {
                String line = f[p++].trim();
                StringTokenizer t = new StringTokenizer(line);
                String token = t.nextToken();
                switch (token)
                {
                case "WIDTH":
                    WIDTH = Integer.parseInt(t.nextToken());
                    break;
                case "HEIGHT":
                    HEIGHT = Integer.parseInt(t.nextToken());
                    break;
                case "XPOS":
                    XPOS = Integer.parseInt(t.nextToken());
                    break;
                case "YPOS":
                    YPOS = Integer.parseInt(t.nextToken());
                    break;
                case "BGCOL":
                    BGCOL = Integer.parseInt(t.nextToken());
                    break;
                case "SCREEN":
                    SCREEN = Integer.parseInt(t.nextToken());
                    break;
                case "CHARAVAIL":
                    CHARAVAIL = new int[2];
                    CHARAVAIL[0] = Integer.parseInt(t.nextToken());
                    CHARAVAIL[1] = Integer.parseInt(t.nextToken());
                    break;
                case "PRG":
                    PRG = Integer.parseInt(t.nextToken());
                    break;
                case "IMAGE":
                    IMAGE = new int[8*WIDTH][8*HEIGHT];
                    for (int y=0;y<8*HEIGHT;y++)
                    {
                        line = f[p++].trim();
                        for (int x=0;x<8*WIDTH;x++)
                            IMAGE[x][y] = Integer.parseInt(line.substring(x,x+1),16);
                    }
                    break;
                default:
                    System.err.println("Unknown token: "+token);
                    System.exit(-1);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public String write()
    {
        StringBuffer b = new StringBuffer((IMAGE.length+1)*(IMAGE[0].length+1));
        for (int j=0;j<IMAGE[0].length;j++)
        {
            for (int i=0;i<IMAGE.length;i++)
                b.append(Integer.toHexString(IMAGE[i][j]).toUpperCase());
            b.append("\n");
        }
        return b.toString();
    }

    public int[][] get_goal()
    {
        int[][] goal = new int[8*WIDTH][8*HEIGHT];
        for (int i=0;i<8*WIDTH;i++)
            for (int j=0;j<8*HEIGHT;j++)
                goal[i][j] = IMAGE[i][j];
        return goal;
    }
}

class Data
{
    public Chars chars;
    public Sprites sprites;

    public Data(int w, int h)
    {
        chars = new Chars(w,h);
        sprites = new Sprites();
    }

    public Data(Data d)
    {
        chars = new Chars(d.chars);
        sprites = new Sprites(d.sprites);
    }

    public int get_trans_background(int i, int j)
    {
        return chars.get_fg_pixel(i,j);
    }

    public int get_background(int nr, int i, int j, int bg)
    {
        for (int s=nr+1;s<=7;s++)
        {
            int pixel = sprites.sprite[s].get_pixel(i,j,sprites.col1,sprites.col2);
            if (pixel!=-1)
            {
                if (sprites.sprite[s].bg==1)
                {
                    int fg_pixel = chars.get_fg_pixel(i,j);
                    if (fg_pixel!=-1) return fg_pixel;
                }
                return pixel;
            }
        }

        int pixel = chars.get_pixel(i,j);
        return pixel==-1?bg:pixel;
    }

    public int get_pixel(int i, int j, int bg)
    {
        for (int s=0;s<=7;s++)
        {
            int pixel = sprites.sprite[s].get_pixel(i,j,sprites.col1,sprites.col2);
            if (pixel!=-1)
            {
                if (sprites.sprite[s].bg==1)
                {
                    int fg_pixel = chars.get_fg_pixel(i,j);
                    if (fg_pixel!=-1) return fg_pixel;
                }
                return pixel;
            }
        }

        int pixel = chars.get_pixel(i,j);
        return pixel==-1?bg:pixel;
    }

    public int get_other(int i, int j)
    {
        for (int s=0;s<=7;s++)
        {
            int pixel = sprites.sprite[s].get_pixel(i,j,sprites.col1,sprites.col2);
            if (pixel!=-1) return sprites.sprite[s].bg==0?-1:pixel;
        }
        return -2;
    }

    public String dump()
    {
        return chars.dump() + sprites.dump();
    }

    public void read_dump(String filename)
    {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            String[] f = lines.toArray(new String[lines.size()]);

            int ss = chars.read_dump(f);
            sprites.read_dump(f,ss);
        } catch (IOException e) { e.printStackTrace(); }
    }
}

class Chars
{
    public Char[][] chr;
    public int mode;
    public int col1;
    public int col2;

    private int width, height;

    public Chars(int w, int h)
    {
        width = w;
        height = h;
        chr = new Char[w][h];
        for (int i=0;i<w;i++)
            for (int j=0;j<h;j++)
                chr[i][j] = new Char();
    }

    public Chars(Chars c)
    {
        mode = c.mode;
        col1 = c.col1;
        col2 = c.col2;

        width = c.width;
        height = c.height;

        chr = new Char[width][height];
        for (int i=0;i<width;i++)
            for (int j=0;j<height;j++)
                chr[i][j] = new Char(c.chr[i][j]);
    }

    public int get_pixel(int i, int j)
    {
        int x = i/8;
        int y = j/8;
        i %= 8;
        j %= 8;
        if (mode==0 || chr[x][y].c<8)
            return chr[x][y].p[i][j]==1?chr[x][y].c:-1;

        if (i%2==1) i--;
        int c = chr[x][y].p[i][j]*2+chr[x][y].p[i+1][j];
        if (c==1) return col1;
        if (c==2) return col2;
        if (c==3) return chr[x][y].c-8;
        return -1;
    }

    public int get_fg_pixel(int i, int j)
    {
        int x = i/8;
        int y = j/8;
        i %= 8;
        j %= 8;
        if (mode==0 || chr[x][y].c<8)
            return chr[x][y].p[i][j]==1?chr[x][y].c:-1;

        if (i%2==1) i--;
        int c = chr[x][y].p[i][j]*2+chr[x][y].p[i+1][j];
        if (c==2) return col2;
        if (c==3) return chr[x][y].c-8;
        return -1;
    }

    public String dump()
    {
        StringBuffer b = new StringBuffer();
        b.append("CHARDUMP\n");
        b.append(mode+" "+col1+" "+col2+" "+width+" "+height+"\n");
        for (int i=0;i<width;i++)
            for (int j=0;j<height;j++)
            {
                b.append("CHAR @("+i+","+j+") col="+chr[i][j].c+"\n");
                for (int jj=0;jj<8;jj++)
                {
                    for (int ii=0;ii<8;ii++)
                        b.append(chr[i][j].p[ii][jj]);
                    b.append("\n");
                }
            }
        return b.toString();
    }

    public int read_dump(String[] f)
    {
        int p = 2;
        StringTokenizer t = new StringTokenizer(f[2].trim());
        mode = Integer.parseInt(t.nextToken());
        col1 = Integer.parseInt(t.nextToken());
        col2 = Integer.parseInt(t.nextToken());

        for (int i=0;i<width;i++)
            for (int j=0;j<height;j++)
            {
                t = new StringTokenizer(f[3+height*9*i+9*j].trim());
                t.nextToken();
                t.nextToken();
                t = new StringTokenizer(t.nextToken(),"=");
                t.nextToken();
                chr[i][j].c = Integer.parseInt(t.nextToken());
                for (int jj=0;jj<8;jj++)
                    for (int ii=0;ii<8;ii++)
                        chr[i][j].p[ii][jj] = f[4+height*9*i+9*j+jj].charAt(ii)-'0';
            }
        return 3+width*9*height;
    }
}

class Char
{
    int[][] p;
    int c;

    public Char()
    {
        p = new int[8][8];
    }

    public Char(Char cc)
    {
        c = cc.c;
        p = new int[8][8];
        for (int i=0;i<8;i++)
            for (int j=0;j<8;j++)
                p[i][j] = cc.p[i][j];
    }
}

class Sprites
{
    public Sprite[] sprite;
    public int col1;
    public int col2;

    public Sprites()
    {
        clear();
    }

    public Sprites(Sprites s)
    {
        col1 = s.col1;
        col2 = s.col2;

        sprite = new Sprite[8];
        for (int i=0;i<8;i++)
            sprite[i] = new Sprite(s.sprite[i]);
    }

    public void clear()
    {
        sprite = new Sprite[8];
        for (int i=0;i<8;i++)
            sprite[i] = new Sprite();
    }

    public int get_bg()
    {
        int val = 0;
        for (int s=0;s<8;s++)
            if (sprite[s].bg==1) val |= 1<<s;
        return val;
    }

    public int get_mc()
    {
        int val = 0;
        for (int s=0;s<8;s++)
            if (sprite[s].mode==1) val |= 1<<s;
        return val;
    }

    public int get_xs()
    {
        int val = 0;
        for (int s=0;s<8;s++)
            if (sprite[s].xs==1) val |= 1<<s;
        return val;
    }

    public int get_ys()
    {
        int val = 0;
        for (int s=0;s<8;s++)
            if (sprite[s].ys==1) val |= 1<<s;
        return val;
    }

    public String dump()
    {
        StringBuffer b = new StringBuffer();
        b.append("SPRITEDUMP\n");
        b.append(col1+" "+col2+"\n");
        for (int s=0;s<8;s++)
        {
            b.append("SPRITE"+s+" @("+sprite[s].x+","+sprite[s].y+") "
                             +sprite[s].xs+" "+sprite[s].ys+" "+sprite[s].bg+" "+sprite[s].col+" "+sprite[s].mode+"\n");
            for (int jj=0;jj<Sprite.HEIGHT;jj++)
            {
                for (int ii=0;ii<24;ii++)
                    b.append(sprite[s].d[ii][jj]);
                b.append("\n");
            }
        }
        return b.toString();
    }

    public void read_dump(String[] f, int ss)
    {
        StringTokenizer t = new StringTokenizer(f[ss+1]);
        col1 = Integer.parseInt(t.nextToken());
        col2 = Integer.parseInt(t.nextToken());
        for (int s=0;s<=7;s++)
        {
            t = new StringTokenizer(f[ss+2+22*s]);
            t.nextToken();
            String tmp = t.nextToken();
            sprite[s].xs = Integer.parseInt(t.nextToken());
            sprite[s].ys = Integer.parseInt(t.nextToken());
            sprite[s].bg = Integer.parseInt(t.nextToken());
            sprite[s].col = Integer.parseInt(t.nextToken());
            sprite[s].mode = Integer.parseInt(t.nextToken());
            t = new StringTokenizer(tmp.substring(2,tmp.length()-1),",");
            sprite[s].x = Integer.parseInt(t.nextToken());
            sprite[s].y = Integer.parseInt(t.nextToken());

            for (int jj=0;jj<Sprite.HEIGHT;jj++)
                for (int ii=0;ii<24;ii++)
                    sprite[s].d[ii][jj] = f[ss+3+22*s+jj].charAt(ii)-'0';
        }
    }
}

class Sprite
{
    static int HEIGHT = 0;

    public int[][] d;
    public int x,y;
    public int xs,ys;
    public int bg;
    public int col;
    public int mode;

    public Sprite()
    {
        d = new int[24][HEIGHT];
    }

    public Sprite(Sprite s)
    {
        x = s.x;
        y = s.y;
        xs = s.xs;
        ys = s.ys;
        bg = s.bg;
        col = s.col;
        mode = s.mode;

        d = new int[24][HEIGHT];
        for (int i=0;i<24;i++)
            for (int j=0;j<HEIGHT;j++)
                d[i][j] = s.d[i][j];
    }

    public void add_to_plane(int[][] plane)
    {
        if (mode==0)
        {
            for (int i=0;i<24;i++)
                for (int j=0;j<HEIGHT;j++)
                    if (d[i][j]==1)
                        for (int ii=0;ii<=xs;ii++)
                            for (int jj=0;jj<=ys;jj++)
                                plane[x+i+xs*i+ii][y+j+ys*j+jj] = 1;
        }
        else
        {
            for (int i=0;i<24;i+=2)
                for (int j=0;j<HEIGHT;j++)
                    if (d[i][j]==1 || d[i+1][j]==1)
                        for (int ii=0;ii<=xs;ii++)
                            for (int jj=0;jj<=ys;jj++)
                                for (int k=0;k<=1;k++)
                                    plane[x+(xs+1)*(i+k)+ii][y+(ys+1)*j+jj] = 1;
        }
    }

    public int get_pixel(int i, int j, int col1, int col2)
    {
        i -= x;
        j -= y;
        if (i<0 || j<0) return -1;
        if (xs==1) i/=2;
        if (ys==1) j/=2;
        if (i>=24 || j>=HEIGHT) return -1;
        if (mode==0)
            return d[i][j]==1?col:-1;

        if (i%2==1) i--;

        int c = d[i][j]*2+d[i+1][j];
        if (c==1) return col1;
        if (c==2) return col;
        if (c==3) return col2;

        return -1;
    }

    public String get_data(int offset)
    {
        String s = "";
        for (int j=0;j<21;j++)
            for (int i=0;i<24;i+=8)
            {
                int val = 0;
                for (int k=0;k<8;k++)
                {
                    val *= 2;
                    if (j+offset>=0 && j+offset<HEIGHT)
                        val += d[i+k][j+offset];
                }
                s += val+",";
            }
        return s.substring(0,s.length()-1);
    }

    public String status()
    {
        return "@"+x+","+y+" "+xs+" "+ys+" "+(bg==1?"bg":"fg")+" "+col+" "+mode;
    }
}

class Charset
{
    public int[][][] charset;
    public int charset_start;
    public int charset_end;
}

class Switch
{
    static int INC = -1;
    static int DEC = -2;

    int from_line, from_cycle, until_line, until_cycle;
    int cell, value;

    public Switch(int cell, int value, int from_line, int from_cycle, int until_line, int until_cycle)
    {
        this.cell = cell;
        this.value = value;
        this.from_line = from_line;
        this.from_cycle = from_cycle;
        this.until_line = until_line;
        this.until_cycle = until_cycle;
    }

    public String toString()
    {
        StringBuffer b = new StringBuffer();
        if (value==DEC)
            b.append("DEC "+cell);
        else if (value==INC)
            b.append("INC "+cell);
        else
            b.append("cell="+value);
        b.append(" @ ("+from_line+"/"+from_cycle+") - ("+until_line+"/"+until_cycle+")");

        return b.toString();
    }

    public static String calc_schedule(List<Switch> l)
    {
        int[] schedule = new int[4000]; // Anzahl der Taktzyklen
        for (int i=0;i<4000;i++)
            schedule[i] = -1;

        for (int nr=0;nr<l.size();nr++)
        {
            Switch s = l.get(nr);
            int from_pos = get_pos(s.from_line,s.from_cycle);
            int until_pos = get_pos(s.until_line,s.until_cycle)-5;

            boolean found = false;
            for (int i=from_pos;i<=until_pos;i++)
            {
                boolean ok = true;
                for (int k=0;k<6;k++)
                    if (schedule[i+k]!=-1)
                    {
                        ok = false;
                        break;
                    }

                if (ok)
                {
                    found = true;
                    schedule[i] = nr;
                    for (int k=1;k<6;k++)
                        schedule[i+k] = -2;
                    break;
                }
            }

            if (!found)
                System.err.println("Could not schedule "+s+" ("+from_pos+" - "+until_pos+")");
        }

        StringBuffer b = new StringBuffer();

        int wait = 0;
        for (int i=0;i<4000;i++)
        {
            if (schedule[i]==-1) wait++;
            else
            {
                if (wait>0) { b.append(" +WAIT "+wait+"\n"); wait=0; }
                if (schedule[i]>=0)
                {
                    Switch s = l.get(schedule[i]);
                    if (s.value==DEC)
                        b.append(" dec "+s.cell+"\n");
                    else if (s.value==INC)
                        b.append(" inc "+s.cell+"\n");
                    else
                        b.append(" lda #"+s.value+"\n sta "+s.cell+"\n");
                }
            }
        }

        int line = 60;
        int cycle = -1;
//      System.err.print(line+": ");
        for (int i=0;i<4000;i++)
        {
            cycle++;
            if (line%8==3)
            {
                if (cycle>0)
                {
                    cycle = 0;
                    line++;
//                  System.err.println();
//                  System.err.print(line+": ");
                }
            }
            else
            {
                if (cycle>43)
                {
                    cycle = 0;
                    line++;
//                  System.err.println();
//                  System.err.print(line+": ");
                }
            }
            /*
            if (schedule[i]==-1) System.err.print(".");
            else if (schedule[i]==-2) System.err.print("#");
            else
            {
                Switch s = l.get(schedule[i]);
                if (s.value==DEC)
                    System.err.print("D");
                if (s.value==INC)
                    System.err.print("I");
                else
                    System.err.print("S");
            }
             */
        }
        //System.err.println();

        return b.toString();
    }

    private static int get_pos(int line, int cycle)
    {
        line -= 60;
        cycle -= 11;
        int pos = 0;
        while (line>0)
        {
            line--;
            if ((line+60)%8==3)
                pos++;
            else
                pos+=44;
        }
        return pos+cycle;
    }
}
